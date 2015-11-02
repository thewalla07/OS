import javax.sound.sampled.*;
import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

class Player extends Panel implements Runnable {
    private static final long serialVersionUID = 2L;
    private TextField textfield;
    private TextArea textarea;
    private Font font;
    private String filename;

    public Player(String filename){
        // building the window for the player
        font = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        textfield = new TextField();
        textarea = new TextArea();
        textarea.setFont(font);
        textfield.setFont(font);
        setLayout(new BorderLayout());
        add(BorderLayout.SOUTH, textfield);
        add(BorderLayout.CENTER, textarea);


        //listens for input from keyboard, add listener to view
        textfield.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                textarea.append("You said: " + e.getActionCommand() + "\n");
                textfield.setText("");
            }
        });

        //get filename from Player init to be played
        this.filename = filename;

        // once setup, start the thread
        new Thread(this).start();
    }

    public void run() {

        try {
            AudioInputStream s = AudioSystem.getAudioInputStream(new File(filename));
            AudioFormat format = s.getFormat();     

            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                throw new UnsupportedAudioFileException();
            }

            int oneSecond = (int) (format.getChannels() * format.getSampleRate() * format.getSampleSizeInBits() / 8);
            
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            
            line.open(format);
            line.start();

            BoundedBuffer b = new BoundedBuffer(oneSecond);
            
            Thread pthread = new Thread(new Producer(b, s, oneSecond));
            pthread.start();

            Thread cthread = new Thread(new Consumer(b, line, oneSecond));
            cthread.start();
            
            pthread.join();
            cthread.join();
            
            line.drain();
            line.stop();
            line.close();
            
            

        } catch (UnsupportedAudioFileException e ) {
            System.out.println("Player initialisation failed");
            e.printStackTrace();
            System.exit(1);
        } catch (LineUnavailableException e) {
            System.out.println("Player initialisation failed");
            e.printStackTrace();
            System.exit(1);
        } catch (IOException e) {
            System.out.println("Player initialisation failed");
            e.printStackTrace();
            System.exit(1);
        } catch (InterruptedException e){
            System.out.println("IE exception thrown");
            e.printStackTrace();
            System.exit(1);
        }
    }
}

public class StudentPlayerApplet extends Applet
{
    private static final long serialVersionUID = 1L;
    public void init() {
        setLayout(new BorderLayout());
        add(BorderLayout.CENTER, new Player(getParameter("file")));
    }
}

class Producer implements Runnable{ // read into buffer from AudioInputStream
    public BoundedBuffer buffer;
    public AudioInputStream s;
    private int oneSecond;

    public Producer(BoundedBuffer buffer0, AudioInputStream stream0, int oneSecond0){
        this.buffer = buffer0;
        this.s = stream0;
        this.oneSecond = oneSecond0;
    }

    public void run(){

        try{
            byte[] audioChunk = new byte[oneSecond];
            int bytesRead = s.read(audioChunk, 0, oneSecond);

            while(bytesRead!=-1){
                buffer.insertChunk(audioChunk);
                bytesRead = s.read(audioChunk, 0, oneSecond);
            }
        }
        catch (IOException e) {
            System.out.println("Player initialisation failed");
            e.printStackTrace();
            System.exit(1);
        }
    }
}

class Consumer implements Runnable{ //write to audio line
    public BoundedBuffer buffer;
    public SourceDataLine line;
    private int oneSecond;

    public Consumer(BoundedBuffer b0, SourceDataLine line0, int oneSecond0){
        this.buffer = b0;
        this.line=line0;
        this.oneSecond = oneSecond0;
    }

    public void run(){
        byte[] audioChunk = buffer.removeChunk();
        while(true){
            line.write(audioChunk, 0, oneSecond);
            audioChunk = buffer.removeChunk();
            System.out.println(oneSecond);
        }
    }
}

class BoundedBuffer { // buffer could know about stream size
    private int nextIn, nextOut, size, occupied, ins, outs, sec;
    private boolean dataAvailable, roomAvailable;
    private byte[][] buffer;

    // what role do ins/outs play?
    BoundedBuffer(int n) {// n should be one second
        nextIn = 0; nextOut = 1; size = n; occupied = 0; ins = 0; outs = 0;
        dataAvailable = false;
        roomAvailable = true;
        sec=10;
        buffer = new byte[sec][size];  
    }

    public synchronized void insertChunk(byte[] audioChunk) {
        try {
            while (!roomAvailable) {wait();}
            roomAvailable=false;
            buffer[nextIn]=audioChunk;
            nextIn = (nextIn + 1) % sec;
            occupied++; 
            ins++; 
            dataAvailable = true; 
            //roomAvailable = (occupied < sec-1) ? true : false;
            if(occupied<sec)roomAvailable=true;
            /*


            so the problem seems to be that the way boundedbuffer is working right now, the buffer fills up to 10 seconds quickly, and then when the buffer is full it is writing over one extra spot. the consumer always ends up taking the most recently placed item instead of the oldest. Need to change something with the array placement to stop overwriting from occuring


            next we may need to add a mixer so that volume can be changed instead of just playing from data line. this will allow pause and play.




            */
            notifyAll();
        } catch (InterruptedException e) { }
    }

    public synchronized byte[] removeChunk() {
        try {
            while (!dataAvailable) {wait();}
            dataAvailable=false;
            byte[] audioChunk = buffer[nextOut];
            nextOut = (nextOut + 1) % sec;
            occupied--; 
            outs++; 
            roomAvailable = true; 
            if(occupied > 0) dataAvailable=true;
            notifyAll();
            return audioChunk;
        } catch (InterruptedException e) {
            System.out.println("Returned null audio chunk from removeChunk()");
            return null;}
    }
}