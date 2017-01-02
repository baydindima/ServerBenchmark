package edu.spbau.master.java.bemchmark.servers.server;

/**
 * Interface for server
 */
public interface Server {
    /**
     * Start server.
     *
     * @param portNum listening port.
     */
    void start(int portNum);


    /**
     * Stop server.
     */
    void stop();
}
