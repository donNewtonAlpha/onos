package org.onosproject.tor;

/**
 * Created by nick on 7/13/15.
 */

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.Callable;

public class DnsProxy {

    private static InetAddress dnsIp;

    public static void initiate(){
        try{

            byte[] dns={4,2,2,2};
            dnsIp = InetAddress.getByAddress(dns);

        }catch(Exception e){
            TorComponent.log.error("Dns Proxy initiation failed", e);
        }
    }

    public static Callable<byte[]> newResolution(byte[] request){

        return new Callable<byte[]>(){
            public byte[] call() throws Exception{
                return resolveDns(request);
            }
        };

    }




    public static byte[] resolveDns(byte[]  message){
        try{

            DatagramSocket send=new DatagramSocket();
            DatagramPacket oPacket=new DatagramPacket(message,message.length,dnsIp,53);
            send.send(oPacket);
            DatagramPacket iPacket=new DatagramPacket(new byte[500],500);
            send.receive(iPacket);
            send.close();
            byte[] response =  new byte[iPacket.getLength()];
            byte[] packetResponse = iPacket.getData();
            for(int i  =0; i < response.length; i++){
                response[i] = packetResponse[i];
            }
            return response;

        }catch(Exception e){
            TorComponent.log.error("dns resolution exception", e);
        }
        return new byte[1];
    }

    static void switchResponse(byte[] dnsResponse, String redirectUrl){

        InetAddress redirectIp = null;

        try {
           redirectIp = InetAddress.getByName(redirectUrl);
        }catch (Exception e) {
            try {
                redirectIp = InetAddress.getByName("127.0.0.1");
            }catch (Exception e2) {
            }
            TorComponent.log.warn("Could not resolve the redirect address",  e);
        }

        try{

            int[] ipIndexes= findIpAnswersIndexes(dnsResponse);
            TorComponent.log.info("ip indexes found");

            byte[] ipBytes = redirectIp.getAddress();

            for(int i = 0; i<4; i++){
                for(int j = 0; j <  ipIndexes.length; j++){
                    dnsResponse[ipIndexes[j]+i] = ipBytes[i];
                }
            }

        }catch (Exception e3) {
            TorComponent.log.error("switchResponse error", e3);
        }
    }

    private static int passName(int startIndex, byte[] dnsMessage){

        int i = startIndex;
        boolean b = true;
        while(b) {
            if ((dnsMessage[i] & 0xc) == 0xc) {
                // pointer to previous name
                b = false;
                i = i + 2;

            }
            if(dnsMessage[i] == 0){
                b = false;
            }
            else if(dnsMessage[i] > 0){
                i = i + dnsMessage[i] + 1;
            }else{
                i = i + 256 + dnsMessage[i] + 1;
            }

        }

        return i;
    }

    private static int[] findIpAnswersIndexes(byte[] dnsResponse){

        int questions = 256*dnsResponse[4] + dnsResponse[5];
        int answers= 256*dnsResponse[6] + dnsResponse[7];
        int[] indexes = new int[answers];
        int x = 0;

        int i = 12;

        for(int j = 0; j<questions; j++){
            i = passName(i, dnsResponse);
            i = i +2;
        }
        // all questions passed
        for (int j = 0;  j <answers; j++){
            i = passName(i, dnsResponse);
            i = i + 10;
            indexes[x] = i;
        }

        return indexes;
    }

    static  String extractDomainNameFromRequest(byte[] message){
        //TODO: improve
        int length;
        int i = 12;
        StringBuilder domainName = new StringBuilder();

        while((i<message.length-4)&& (message[i] != 0x00)){
            length = message[i];
            i++;
            for(int j =  0; j< length; j++){
                domainName.append((char) message[i +j]);
            }
            i += length;
            if(message[i] != 0x00){
                domainName.append('.');
            }
        }


        return domainName.toString();
    }
}
