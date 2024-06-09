package bate.papo.msn;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class Cliente3 {
    static boolean conectado = true; //Variável para controlar o estado de conexão do cliente

    public static void main(String argv[]) throws Exception {

        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
        String nomeUsuario = null;

        // Solicita o nome do usuário
        System.out.print("Digite seu nome: ");
        nomeUsuario = inFromUser.readLine();

        // Conecta ao servidor na porta 6789
        Socket clientSocket = new Socket("localhost", 6789);

        // Cria streams de entrada e saída para comunicação com o servidor
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        // Envia o nome do usuário para o servidor
        outToServer.writeBytes(nomeUsuario + '\n');
        String finalNomeUsuario = nomeUsuario;

        // Runnable para enviar mensagens ao servidor
        Runnable sendHandler = () -> {
            while (conectado) {
                String sentence = null;
                try {
                    sentence = inFromUser.readLine();

                    // Se o usuário digitar "sair", desconecta do chat
                    if (sentence.equalsIgnoreCase("sair")) {
                        System.out.println("Você saiu do chat.");
                        outToServer.writeBytes(finalNomeUsuario + ": " + sentence + '\n');
                        conectado = false;
                    }

                    // Envia a mensagem para o servidor
                    try {
                        outToServer.writeBytes(finalNomeUsuario + ": " + sentence + '\n');
                    } catch (IOException e) {
                        // Captura exceção de I/O (entrada/saída)
                    }
                } catch (IOException e) {
                    // Captura exceção de I/O
                }
            }
            System.out.println("Conexão finalizada pelo usuário.");
        };

        // Runnable para receber mensagens do servidor
        Runnable receiveHandler = () -> {
            while (conectado) {
                String mensagemDoServidor = null;
                try {
                    // Lê mensagens do servidor se disponíveis
                    if (inFromServer.ready()) {
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

            // Lê a primeira mensagem do servidor
            mensagemDoServidor = inFromServer.readLine();

            if (mensagemDoServidor.equalsIgnoreCase("OK")) {
                // Se a conexão for aceita, inicia as threads de envio e recebimento
                Thread receiveThread = new Thread(receiveHandler);
                Thread sendThread = new Thread(sendHandler);
                System.out.println("Conexão aceita.");
                receiveThread.start();
                sendThread.start();
            } else {
                // Se a conexão for recusada, fecha o socket do cliente
                System.out.println("Conexão recusada.");
                clientSocket.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
