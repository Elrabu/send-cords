package org.game;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.xml.crypto.Data;
import java.io.*;
import java.net.*;

public class PlayerFrame extends JFrame{

    private int width, height;
    private Container contentPane;
    private PlayerSprite me;
    private PlayerSprite enemy;
    private DrawingComponent dc;
    private Timer animationTimer;
    private boolean up, down, left, right;
    private Socket socket;
    private int playerID;
    private ReadFromServer rfsRunnable;
    private WriteToServer wtsRunnable;
    private String hostIpAdress = "";

    public PlayerFrame(int w, int h){
        width = w;
        height = h;
        up = false;
        down = false;
        left = false;
        right = false;
    }

    public void setUpGUI() {
        contentPane = this.getContentPane();
        this.setTitle("Player #" + playerID);
        contentPane.setPreferredSize(new Dimension(width,height));
        createSprites();
        dc = new DrawingComponent();
        contentPane.add(dc);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.pack();
        this.setVisible(true);

        setUpAnimationTimer();
        setUpKeyListener();
    }

    private void createSprites() {
        if(playerID == 1) {
            me = new PlayerSprite(100, 400, 50, Color.BLUE);
            enemy = new PlayerSprite(490, 400, 50, Color.RED);
        } else {
            me = new PlayerSprite(490,400,50,Color.RED);
            enemy = new PlayerSprite(100,400,50,Color.BLUE);
        }

    }

    private void setUpAnimationTimer() {
        int interval = 10;
        ActionListener al = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                double speed = 5;
                if(up) {
                    me.moveV(-speed);
                } else if(down) {
                    me.moveV(speed);
                } else if(left) {
                    me.moveH(-speed);
                }else if(right) {
                    me.moveH(speed);
                }
                dc.repaint();
            }
        };
        animationTimer = new Timer(interval, al);
        animationTimer.start();
    }

    private void setUpKeyListener() {
        KeyListener kl = new KeyListener() {
            public void keyTyped(KeyEvent ke) {

            }

            public void keyPressed(KeyEvent ke) {
                int keyCode = ke.getKeyCode();

                switch (keyCode) {
                    case KeyEvent.VK_UP :
                        up = true;
                        break;
                    case KeyEvent.VK_DOWN:
                        down = true;
                        break;
                    case KeyEvent.VK_LEFT:
                        left = true;
                        break;
                    case KeyEvent.VK_RIGHT:
                        right = true;
                        break;
                }
            }

            public void keyReleased(KeyEvent ke) {
                int keyCode = ke.getKeyCode();

                switch (keyCode) {
                    case KeyEvent.VK_UP :
                        up = false;
                        break;
                    case KeyEvent.VK_DOWN:
                        down = false;
                        break;
                    case KeyEvent.VK_LEFT:
                        left = false;
                        break;
                    case KeyEvent.VK_RIGHT:
                        right = false;
                        break;
                }
            }
        };
        contentPane.addKeyListener(kl);
        contentPane.setFocusable(true);
    }

    private void connectToServer() {
        try {
            socket = new Socket(hostIpAdress, 45371);                //Connect to the Server
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            playerID = in.readInt();
            System.out.println("You are Player #" + playerID);
            if(playerID == 1) {
                System.out.println("Waiting for Player #2 to connect...");
            }
            rfsRunnable = new ReadFromServer(in);
            wtsRunnable = new WriteToServer(out);
            rfsRunnable.waitForStartMsg();
        }catch(IOException ex){
            System.out.println("IOEXCeption form connectServer()");
        }
    }

    private class DrawingComponent extends JComponent {
        protected void paintComponent(Graphics g){
            Graphics2D g2d = (Graphics2D) g;
            enemy.drawSprite(g2d);
            me.drawSprite(g2d);
        }
    }

    private class ReadFromServer implements Runnable {
        private DataInputStream dataIn;

        public ReadFromServer(DataInputStream in){
            dataIn = in;
            System.out.println("RFS Runnable created");
        }

        public void run() {
            try {
                while(true) {
                    if(enemy != null) {
                        enemy.setX(dataIn.readDouble());
                        enemy.setY(dataIn.readDouble());
                    }
                }
            } catch(IOException ex) {
                System.out.println("IOException RFS run()");
            }
        }

        public void waitForStartMsg() {
            try {
                String startMsg = dataIn.readUTF();
                System.out.println("Message from server: "+ startMsg);
                Thread readThread = new Thread(rfsRunnable);
                Thread writeThread = new Thread(wtsRunnable);
                readThread.start();
                writeThread.start();

            } catch(IOException ex) {
                System.out.println("IOException from waitForStartMsg()");
            }
        }

    }

    private class WriteToServer implements Runnable {
        private DataOutputStream dataOut;

        public WriteToServer(DataOutputStream out){
            dataOut = out;
            System.out.println("WTS Runnable created");
        }

        public void run() {
            try {

                while (true){
                    if(me != null) {
                        dataOut.writeDouble(me.getX());
                        dataOut.writeDouble(me.getY());
                        dataOut.flush();
                    }
                    try {
                        Thread.sleep(25);
                    }catch (InterruptedException ex){
                        System.out.println("InterruptedException from WTS run()");
                    }
                }

            }catch (IOException ex) {
                System.out.println("IoException from WTS run(");
            }
        }

    }

    public static void main(String[] args) {
        PlayerFrame pf = new PlayerFrame(640, 480);
        pf.connectToServer();
        pf.setUpGUI();
    }
}
