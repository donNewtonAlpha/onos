package org.onosproject.vcpe;

/**
 * Created by nick on 7/13/15.
 */

import java.net.*;
import java.util.concurrent.Callable;

public class DnsProxy{

    private static InetAddress dnsIp;

    public static void initiate(){
        try{

            byte[] dns={4,2,2,2};

            dnsIp = InetAddress.getByAddress(dns);

            ParseStructure.initiate();

        }catch(Exception e){
            VcpeComponent.log.error("Dns Proxy initiation failed", e);
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
            VcpeComponent.log.info("socket created");
            DatagramPacket oPacket=new DatagramPacket(message,message.length,dnsIp,53);
            VcpeComponent.log.info("outbound packet created");
            send.send(oPacket);
            VcpeComponent.log.info("packet sent");
            DatagramPacket iPacket=new DatagramPacket(new byte[500],500);
            VcpeComponent.log.info("empty incoming packet created");
            send.receive(iPacket);
            VcpeComponent.log.info("packet received");
            send.close();
            byte[] response =  new byte[iPacket.getLength()];
            byte[] packetResponse = iPacket.getData();
            for(int i  =0; i < response.length; i++){
                response[i] = packetResponse[i];
            }
            return response;

        }catch(Exception e){
            VcpeComponent.log.error("dns resolution exception", e);
        }
        return new byte[1];
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
