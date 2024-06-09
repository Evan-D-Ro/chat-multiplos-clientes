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
        LinkedList<Usuario> conectados = new LinkedList<>();
        LinkedList<Socket> semaforo = new LinkedList<>();

        ServerSocket welcomeSocket = new ServerSocket(6789);


        Runnable connectionHandler = () -> {
            while (true) {
                try {
                    System.out.println("Waiting for a new connection...");
                    Socket connectionSocket = welcomeSocket.accept();
                    System.out.println("New connection accepted from " + connectionSocket.getInetAddress().getHostAddress());
                    synchronized (semaforo) {
                        semaforo.add(connectionSocket);
                        BufferedReader inFromClient = new BufferedReader(new InputStreamReader(semaforo.getFirst().getInputStream()));
                        Singleton.nome = inFromClient.readLine();
                    }


                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        };

        Thread connectionThread = new Thread(connectionHandler);
        connectionThread.start();

        Runnable userHandler = () -> {
            while (true) {

                String msg = null;
                String msgSubst;
                int colonIndex;
                int quantConectados;

                synchronized (conectados) {
                    quantConectados = conectados.size();
                }
                for (int i = 0; i < quantConectados; i++) {
                    try {
                        Usuario usuarioReceive = conectados.get(i);
                        if (usuarioReceive.getInFromClient().ready()) {
                            msg = usuarioReceive.getInFromClient().readLine();
                            colonIndex = msg.indexOf(':');
                            String command = msg.substring(colonIndex + 2).trim();
                            if (command.equalsIgnoreCase("sair")) {
                                usuarioReceive.getConnectionSocket().close();
                                conectados.remove(i);
                                quantConectados--;
                                for (Usuario users : conectados) {
                                    users.getOutToClient().writeBytes("Mensagem do Servidor: O usuario '" + usuarioReceive.getNome() + "' saiu do chat!" + '\n');
                                }
                                System.out.println("O usuario '"+usuarioReceive.getNome()+"' se desconetou.");
                            } else {
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

        Thread userThread = new Thread(userHandler);
        userThread.start();

        Runnable managerHandler = () -> {
            while (true) {
                try {
                    synchronized (semaforo) {
                        if (!semaforo.isEmpty()) {
                            if (semaforo.getFirst().isConnected()) {
                                if (conectados.isEmpty()) {
                                    Singleton.aux = semaforo.getFirst();
                                    semaforo.removeFirst();
                                    Usuario usuario = new Usuario(true, Singleton.aux);
                                    usuario.setNome(Singleton.nome);
                                    synchronized (conectados) {
                                        conectados.add(usuario);
                                    }
                                    usuario.getOutToClient().writeBytes("OK\n");
                                } else {
                                    Singleton.contador = conectados.size();
                                    for (Usuario users : conectados) {
                                        users.getOutToClient().writeBytes("Mensagem do Servidor: O usuario '" + Singleton.nome + "' esta tentando se conectar (!sim para aceitar e !nao para recusar)" + '\n');
                                    }

                                    while (Singleton.contador > 0) // trava enquanto todos não votarem
                                    {
                                        Thread.sleep(4000); // Pausa a execução da thread por 5 segundos
                                        System.out.println("Aguardando votação finalizar... Faltam: " + Singleton.contador + " usuários votar(em).");
                                    }
                                    System.out.println("-----------------\nVOTAÇÃO FINALIZADA\n-----------------");


                                    boolean accept = true;
                                    for (Usuario usuario : conectados) {
                                        if (!usuario.isAccept()) {
                                            accept = false;
                                        }
                                        usuario.setVote(false);
                                    }

                                    if (accept) {
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
        Thread managerThread = new Thread(managerHandler);
        managerThread.start();

    }
}


