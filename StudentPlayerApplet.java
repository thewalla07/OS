import javax.sound.sampled.*;
import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public class StudentPlayerApplet extends Applet
{
    private static final long serialVersionUID = 1L;
    public void init() {
        setLayout(new BorderLayout());
        add(BorderLayout.CENTER, new Player(getParameter("file")));
    }
}

class Player extends Panel implements Runnable {
    private static final long serialVersionUID = 1L;
    private TextField textfield;
    private TextArea textarea;
    private Font font;
    private String filename;

    // this block moved from run
    // defined them here so can use them in all methods
    private AudioInputStream s;
    private AudioFormat format;
    private DataLine.Info info;
    private int oneSecond;
    private BoundedBuffer b;
    private SourceDataLine line;
    private Producer p;
    private Consumer c;
    private Thread pthread;
    private Thread cthread;
    private FloatControl volCtrl;
    private BooleanControl muteCtrl;
    private Float vol;

    public Player(String filename) {

        font = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        textfield = new TextField();
        textarea = new TextArea();
        textarea.setFont(font);
        textfield.setFont(font);    
        setLayout(new BorderLayout());
        add(BorderLayout.SOUTH, textfield);
        add(BorderLayout.CENTER, textarea);
        
        textfield.addActionListener(
            new ActionListener() {

                public void actionPerformed(ActionEvent e) {

                    String input = e.getActionCommand();
                    switch(input) {

                        case "x":
                            textarea.append("Command received: Halt playback \n");   
                            c.stopConsumer();
                            b.stopBuffer();
                            p.stopProducer();                               
                            break;

                        case "q":
                            // raise volume
                            textarea.append("Command received: Increase Volume \n");
                            if (vol < 5.0206F) {
                                vol = (vol + 1.0F);
                            }
                            volCtrl.setValue(vol);
                            break;

                        case "a":
                            // lower volume 
                            textarea.append("Command received: Decrease Volume \n");
                            if ( vol > -79.0F) {
                                vol = (vol - 1.0F);
                            }
                            volCtrl.setValue(vol);
                            break;

                        case "p":
                            // pause playback
                            textarea.append("Command received: Pause Playback \n");
                            c.pauseConsumer();
                            break;

                        case "r":
                            // resume playback
                             textarea.append("Command received: Resume Playback \n");
                            c.resumeConsumer();
                            break;

                        case "m":
                            // mute 
                            textarea.append("Command received: Mute Audio \n");
                            muteCtrl.setValue(true);
                            break;

                        case "u":
                            // unmute
                            textarea.append("Command received: Unmute Audio \n");
                            muteCtrl.setValue(false);
                            break;

                        default:
                            // default - do nothing wrong input 
                            break;
                    }
                }
            }
        );

        this.filename = filename;
        new Thread(this).start();
    }

    public void run() {

        try {
            s = AudioSystem.getAudioInputStream(new File(filename));
            format = s.getFormat();   
            textarea.append("Audio file: " + filename + "\n" );
            textarea.append("Audio format: " + format.toString() + "\n" );
            int durationInSeconds = (int) ((s.getFrameLength()) / format.getFrameRate());  
            textarea.append("Audio file duration: " + durationInSeconds + " seconds \n" );
            info = new DataLine.Info(SourceDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                throw new UnsupportedAudioFileException();
            }

            oneSecond = (int) (format.getChannels() * format.getSampleRate()
                        * format.getSampleSizeInBits() / 8 );

            b = new BoundedBuffer(oneSecond);

            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format, oneSecond);

            muteCtrl = (BooleanControl) line.getControl(
                        BooleanControl.Type.MUTE);
            volCtrl = (FloatControl) line.getControl(
                        FloatControl.Type.MASTER_GAIN);

            muteCtrl.setValue(false);
            vol = volCtrl.getValue();
            System.out.println(vol);

            line.start();

            p = new Producer(oneSecond, s, b);
            c = new Consumer(oneSecond, line, b);

            pthread = new Thread(p);           
            cthread = new Thread(c);

            pthread.start();
            cthread.start();

            pthread.join();
            textarea.append("Producer says: goodbye \n");
            cthread.join();
            textarea.append("Consumer says: goodbye \n");

            line.drain();
            line.stop();
            textarea.append("Main says: playback complete \n");
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
        } catch (InterruptedException e) {

            System.out.println("Interrupted Exception");
            e.printStackTrace();
            System.exit(1);
        }
    }
}

class Consumer implements Runnable {

    private byte[] audioChunk;
    private int oneSecond, bytesRead;
    private SourceDataLine line;
    private BoundedBuffer b;
    private boolean done, paused;

    Consumer(int oneSecond0, SourceDataLine line0, BoundedBuffer b0) {

        oneSecond = oneSecond0;
        line = line0;
        b = b0;
        audioChunk = new byte[oneSecond];
        bytesRead = 0;
        done = false;
        paused = false;
    }

    public void stopConsumer() {
        done = true;
    }
    
    public void pauseConsumer() {
        b.pauseBuffer();
    }

    public void resumeConsumer() {
        b.resumeBuffer();
    }

    public void run() {

        try {

            while (!done) {
                audioChunk = b.removeChunk();
                while (paused) {
                    try {wait();} catch (InterruptedException f) {}
                }
                line.write(audioChunk, 0, audioChunk.length);
            }

            System.out.println("Bye from Consumer");
        } catch (Exception e) {
            System.out.println("Consumer interrupted");
            e.printStackTrace();
            return;
        }
    }
}

class Producer implements Runnable {

    private byte[] audioChunk;
    private int oneSecond, bytesRead;
    private AudioInputStream s;
    private BoundedBuffer b;
    private boolean done;

    Producer(int oneSecond0, AudioInputStream s0, BoundedBuffer b0) {
        oneSecond = oneSecond0;
        s = s0;
        b = b0;
        audioChunk = new byte[oneSecond];
        bytesRead = 0;
        done = false;
    }

    public void stopProducer() {
        // allows the producer to stop execution
        done = true;
    }

    public void run() {

        try {
            while (!done && bytesRead != -1) {
                bytesRead = s.read(audioChunk);
                b.insertChunk(audioChunk);
            }

            System.out.println("Bye from Producer");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Producer interrupted");
        }
    }
}

class BoundedBuffer {

    private byte[] bufferArray;
    private byte[] transfer;
    private int nextIn, nextOut, amountOccupied, chunkSize, i, j;
    private boolean isFull, isEmpty, paused;

    BoundedBuffer(int chunkSize0) {
        bufferArray = new byte[10 * chunkSize0];
        nextIn = 0;
        nextOut = 0;
        amountOccupied=0;
        chunkSize = chunkSize0;
        isFull = true;
        isEmpty = true;
        transfer = new byte[chunkSize];
        paused = false;
    }

    public synchronized void stopBuffer() {
        // set amountOccupied=0 to allow producer to wake from wait state
        amountOccupied = 0;
        notifyAll();
    }

    public synchronized void pauseBuffer() {
        // set amountOccupied=0 to allow producer to wake from wait state
        paused = true;
        notifyAll();
    }

    public synchronized void resumeBuffer() {
        // set amountOccupied=0 to allow producer to wake from wait state
        paused = false;
        notifyAll();
    }

    public synchronized void insertChunk(byte[] input) {

        try {
            while (amountOccupied == 10) {
                wait();
            }
            for (int i = 0; i < input.length; i++) {
                bufferArray[(nextIn + i) % (chunkSize * 10)] = input[i];
            }
            nextIn += input.length % (chunkSize * 10);
            amountOccupied++;
            notifyAll();
        } catch(InterruptedException e) {
            e.printStackTrace();
            System.out.println("Insert chunk failed");
        } catch(ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
        }
    }

    public synchronized byte[] removeChunk() {

        try{
            while (amountOccupied == 0 || paused) {
                wait();
            }
            for (int j = 0; j < transfer.length; j++) {
                transfer[j] = bufferArray[(nextOut + j) % (chunkSize * 10)];
            }
            nextOut += transfer.length % (chunkSize * 10);
            amountOccupied--;
            notifyAll();
            return transfer;
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.out.println("Remove chunk failed");
            return null;
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
            return null;
        }
    }
}