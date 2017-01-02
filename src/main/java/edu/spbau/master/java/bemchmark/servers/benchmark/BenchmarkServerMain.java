package edu.spbau.master.java.bemchmark.servers.benchmark;

import edu.spbau.master.java.bemchmark.servers.app.MasterServer;

/**
 * Main class for benchmark server.
 */
public final class BenchmarkServerMain {
    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        MasterServer masterServer = new MasterServer();
        masterServer.start(port);
    }
}
