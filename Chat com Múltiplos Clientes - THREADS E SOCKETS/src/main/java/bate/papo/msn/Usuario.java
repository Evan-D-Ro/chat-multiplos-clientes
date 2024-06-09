package bate.papo.msn;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class Usuario{
    private String nome;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    private Socket connectionSocket;
    private BufferedReader inFromClient;
    private DataOutputStream outToClient;
    private boolean conectado;
    private boolean isAccept;
    private boolean isVote;



    public Usuario(boolean conectado, Socket conexao) throws IOException {
        this.conectado = conectado;
        this.connectionSocket = conexao;
        this.inFromClient = new BufferedReader(new InputStreamReader(conexao.getInputStream()));
        this.outToClient = new DataOutputStream(conexao.getOutputStream());
        this.isAccept = true;
    }


    public Socket getConnectionSocket() {
        return connectionSocket;
    }

    public void setConnectionSocket(Socket connectionSocket) {
        this.connectionSocket = connectionSocket;
    }

    public BufferedReader getInFromClient() {
        return inFromClient;
    }

    public void setInFromClient(BufferedReader inFromClient) {
        this.inFromClient = inFromClient;
    }

    public DataOutputStream getOutToClient() {
        return outToClient;
    }

    public void setOutToClient(DataOutputStream outToClient) {
        this.outToClient = outToClient;
    }

    public boolean isConectado() {
        return conectado;
    }

    public void setConectado(boolean conectado) {
        this.conectado = conectado;
    }

    public boolean isAccept() {
        return isAccept;
    }

    public void setAccept(boolean isAccept) {
        this.isAccept = isAccept;
    }


    public boolean isVote() {
        return isVote;
    }

    public void setVote(boolean vote) {
        isVote = vote;
    }
}