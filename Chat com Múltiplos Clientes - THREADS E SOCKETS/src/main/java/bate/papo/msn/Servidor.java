package bate.papo.msn;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

public class Servidor {

    public static void main(String argv[]) throws Exception {
        // Lista para armazenar usuários conectados
        LinkedList<Usuario> conectados = new LinkedList<>();
        // Lista para gerenciar conexões pendentes
        LinkedList<Socket> semaforo = new LinkedList<>();

        // Criação do socket do servidor na porta 6789
        ServerSocket welcomeSocket = new ServerSocket(6789);

        // Runnable para lidar com novas conexões
        Runnable connectionHandler = () -> {
            while (true) {
                try {
                    System.out.println("Waiting for a new connection...");
                    // Aceita novas conexões de clientes
                    Socket connectionSocket = welcomeSocket.accept();
                    System.out.println("New connection accepted from " + connectionSocket.getInetAddress().getHostAddress());

                    // Sincroniza acesso à lista de conexões pendentes
                    synchronized (semaforo) {
                        semaforo.add(connectionSocket);
                        BufferedReader inFromClient = new BufferedReader(new InputStreamReader(semaforo.getFirst().getInputStream()));
                        // Lê o nome do usuário a partir da primeira mensagem
                        Singleton.nome = inFromClient.readLine();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        };

        // Inicia a thread para lidar com novas conexões
        Thread connectionThread = new Thread(connectionHandler);
        connectionThread.start();

        // Runnable para lidar com mensagens dos usuários
        Runnable userHandler = () -> {
            while (true) {
                String msg = null;
                int quantConectados;

                synchronized (conectados) {
                    quantConectados = conectados.size();
                }

                // Loop para verificar mensagens de todos os usuários conectados
                for (int i = 0; i < quantConectados; i++) {
                    try {
                        Usuario usuarioReceive = conectados.get(i);
                        if (usuarioReceive.getInFromClient().ready()) {
                            // Lê a mensagem do usuário
                            msg = usuarioReceive.getInFromClient().readLine();
                            int colonIndex = msg.indexOf(':');
                            String command = msg.substring(colonIndex + 2).trim();
                            // Se o comando for "sair", desconecta o usuário
                            if (command.equalsIgnoreCase("sair")) {
                                usuarioReceive.getConnectionSocket().close();
                                conectados.remove(i);
                                quantConectados--;
                                for (Usuario users : conectados) {
                                    users.getOutToClient().writeBytes("Mensagem do Servidor: O usuario '" + usuarioReceive.getNome() + "' saiu do chat!" + '\n');
                                }
                            } else {
                                // Lida com comandos de votação
                                if (!command.isEmpty() && command.charAt(0) == '!') {
                                    if (!usuarioReceive.isVote() && Singleton.contador > 0) {
                                        if (command.equalsIgnoreCase("!sim")) {
                                            usuarioReceive.setAccept(true);
                                            usuarioReceive.setVote(true);
                                            Singleton.contador--;
                                            System.out.println("Usuário '" + usuarioReceive.getNome() + "' aceitou. isAccept: true, isVote: true");
                                        } else {
                                            if (command.equalsIgnoreCase("!nao")) {
                                                usuarioReceive.setAccept(false);
                                                usuarioReceive.setVote(true);
                                                Singleton.contador--;
                                                System.out.println("Usuário não aceitou. isAccept: false, isVote: true");
                                            } else {
                                                System.out.println("Comando não reconhecido. Mensagem ignorada.");
                                            }
                                        }
                                    }
                                }
                                // Envia a mensagem para todos os outros usuários
                                for (int j = 0; j < quantConectados; j++) {
                                    Usuario usuarioSend = conectados.get(j);
                                    if (usuarioSend != usuarioReceive) {
                                        try {
                                            usuarioSend.getOutToClient().writeBytes(msg + "\n");
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };

        // Inicia a thread para lidar com mensagens dos usuários
        Thread userThread = new Thread(userHandler);
        userThread.start();

        // Runnable para gerenciar a aceitação de novos usuários
        Runnable managerHandler = () -> {

            while (true) {
                try {
                    synchronized (semaforo) {
                        if (!semaforo.isEmpty()) {
                            if (semaforo.getFirst().isConnected()) {
                                if (conectados.isEmpty()) {
                                    // Adiciona o primeiro usuário sem votação, pois está sozinho
                                    Singleton.aux = semaforo.getFirst();
                                    semaforo.removeFirst();
                                    Usuario usuario = new Usuario(true, Singleton.aux);
                                    usuario.setNome(Singleton.nome);
                                    synchronized (conectados) {
                                        conectados.add(usuario);
                                    }
                                    usuario.getOutToClient().writeBytes("OK\n");
                                } else {
                                    // Inicia o processo de votação para novos usuários
                                    Singleton.contador = conectados.size();
                                    for (Usuario users : conectados) {
                                        users.getOutToClient().writeBytes("Mensagem do Servidor: O usuario '" + Singleton.nome + "' esta tentando se conectar (!sim para aceitar e !nao para recusar)" + '\n');
                                    }

                                    // Espera até que todos os usuários votem
                                    while (Singleton.contador > 0) {
                                        Thread.sleep(4000);
                                        System.out.println("Aguardando votação finalizar... Faltam: " + Singleton.contador + " usuários votar(em).");
                                    }
                                    System.out.println("-----------------\nVOTAÇÃO FINALIZADA\n-----------------");

                                    // Verifica o resultado da votação
                                    boolean accept = true;
                                    for (Usuario usuario : conectados) {
                                        if (!usuario.isAccept()) {
                                            accept = false;
                                        }
                                        usuario.setVote(false);
                                    }

                                    if (accept) {
                                        // Aceita a nova conexão
                                        Singleton.aux = semaforo.getFirst();
                                        semaforo.removeFirst();
                                        Usuario usuario = new Usuario(true, Singleton.aux);
                                        usuario.setNome(Singleton.nome);
                                        usuario.getOutToClient().writeBytes("OK\n");
                                        conectados.add(usuario);
                                        for (Usuario users : conectados) {
                                            if (users == usuario) {
                                                users.getOutToClient().writeBytes("Mensagem do Servidor: Bem vindo ao chat " + Singleton.nome + "!" + '\n');
                                            }
                                            users.getOutToClient().writeBytes("Mensagem do Servidor: O usuario '" + Singleton.nome + "' foi aceito no chat" + '\n');
                                        }
                                    } else {
                                        //Rejeita a nova conexão
                                        Usuario usuario = new Usuario(true, semaforo.getFirst());
                                        usuario.getOutToClient().writeBytes("Conexão Recusada.\n");
                                        usuario = null;
                                        semaforo.getFirst().close();
                                        semaforo.removeFirst();
                                        for (Usuario users : conectados) {
                                            users.getOutToClient().writeBytes("Mensagem do Servidor: A entrada do usuario '" + Singleton.nome + "' foi rejeitada, pois alguem votou '!nao!'" + '\n');
                                        }
                                    }
                                }
                            } else {
                                semaforo.getFirst().close();
                                semaforo.removeFirst();
                            }
                        }
                    }
                } catch (Exception ex) {
                    System.out.println(ex);
                }
            }
        };

        // Inicia a thread para gerenciar novas conexões
        Thread managerThread = new Thread(managerHandler);
        managerThread.start();
    }
}
