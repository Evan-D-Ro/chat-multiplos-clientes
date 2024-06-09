package bate.papo.msn;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class Cliente2 {
    static boolean conectado = true;

    public static void main(String argv[]) throws Exception {

        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        String nomeUsuario = null;
        System.out.print("Digite seu nome: ");
        nomeUsuario = inFromUser.readLine();
        Socket clientSocket = new Socket("localhost", 6789);
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        outToServer.writeBytes(nomeUsuario + '\n');
        String finalNomeUsuario = nomeUsuario;
        Runnable sendHandler = () -> {

            while (conectado) {

                String sentence = null;
                try {
                    sentence = inFromUser.readLine();
                    if (sentence.equalsIgnoreCase("sair")) {
                        System.out.println("Você saiu do chat.");
                        outToServer.writeBytes(finalNomeUsuario + ": " + sentence + '\n');
                        conectado = false;
                    }
                    try {
                        outToServer.writeBytes(finalNomeUsuario + ": " + sentence + '\n');
                    }
                    catch(IOException e){

                    }
                } catch (IOException e) {
                }
            }
            System.out.println("Conexão finalizada pelo usuário.");
        };

        Runnable receiveHandler = () -> {
            while (conectado) {
                String mensagemDoServidor = null;
                try {
                    if(inFromServer.ready()) {
                        mensagemDoServidor = inFromServer.readLine();
                        System.out.println(mensagemDoServidor);
                    }

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };


        String mensagemDoServidor = null;
        try {
            System.out.println("Aguardando conexão...");

            mensagemDoServidor = inFromServer.readLine();
            if (mensagemDoServidor.equalsIgnoreCase("OK"))
            {
                Thread receiveThread = new Thread(receiveHandler);
                Thread sendThread = new Thread(sendHandler);
                System.out.println("Conexão aceita.");
                receiveThread.start();
                sendThread.start();


            }
            else
            {
                System.out.println("Conexão recusada.");
                clientSocket.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
