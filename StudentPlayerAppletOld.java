import javax.sound.sampled.*;
import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

class Player extends Panel implements Runnable {
    private static final long serialVersionUID = 1L;
    private TextField textfield;
    private TextArea textarea;
    private Font font;
    private String filename;

    public Player(String filename){

	font = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
	textfield = new TextField();
	textarea = new TextArea();
	textarea.setFont(font);
	textfield.setFont(font);
	setLayout(new BorderLayout());
	add(BorderLayout.SOUTH, textfield);
	add(BorderLayout.CENTER, textarea);

	textfield.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    textarea.append("You said: " + e.getActionCommand() + "\n");
		    textfield.setText("");
		}
	    });

	this.filename = filename;
	new Thread(this).start();
    }

    public void run() {

	try {
		//producer
	    AudioInputStream s = AudioSystem.getAudioInputStream(new File(filename));
	    AudioFormat format = s.getFormat();	    
	    //System.out.println("Audio format: " + format.toString());

	    DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
	    if (!AudioSystem.isLineSupported(info)) {
		throw new UnsupportedAudioFileException();
	    }

	    //get byte size of one seconed of audio from current format
	    int oneSecond = (int) (format.getChannels() * format.getSampleRate() * 
				   format.getSampleSizeInBits() / 8);
	    byte[] audioChunk = new byte[oneSecond];

	    SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
	    line.open(format);
	    line.start();

	    BoundedBuffer b = new BoundedBuffer(oneSecond);

	    int bytesRead = s.read(audioChunk);	
	    b.insertChunk(audioChunk);
	    while(bytesRead!=-1){
		    System.out.println(oneSecond);
		    audioChunk = b.removeChunk();
		    line.write(audioChunk, 0, oneSecond);
		    bytesRead = s.read(audioChunk);
		    b.insertChunk(audioChunk);	

		}
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



class BoundedBuffer { // buffer could know about stream size
    private int nextIn, nextOut, size, occupied, ins, outs;
    private boolean dataAvailable, roomAvailable;
    private byte[][] buffer;

    // what role do ins/outs play?

    /*BoundedBuffer() {
        nextIn = 0; nextOut = 0; size = 10; occupied = 0; ins = 0; outs = 0;
        dataAvailable = false; 0-roomAvailable = true;
        buffer = new byte[size];    
    }*/

    BoundedBuffer(int n) {// n should be one second
        nextIn = 0; nextOut = 0; size = n; occupied = 0; ins = 0; outs = 0;
        dataAvailable = false;
        roomAvailable = true;
        buffer = new byte[10][size];  
    }

    public synchronized void insertChunk(byte[] audioChunk) {
        try {
            while (!roomAvailable) {wait();}
            buffer[nextIn]=audioChunk;
            nextIn = (nextIn + 1) % 10;
            occupied++; 
            ins++; 
            dataAvailable = true; 
            roomAvailable = (occupied < 10) ? true : false;
            notifyAll();
        } catch (InterruptedException e) { }
    }

    public synchronized byte[] removeChunk() {
        try {
            while (!dataAvailable) {wait();}
            byte[] audioChunk = buffer[nextOut];
            //Byte c = buffer[nextOut]; 
            nextOut = (nextOut + 1) % 10;
            occupied--; 
            outs++; 
            roomAvailable = true; 
            dataAvailable = (occupied == 0) ? false : true;
            notifyAll();
            return audioChunk;
        } catch (InterruptedException e) {
            System.out.println("Returned null audio chunk from removeChunk()");
            return null;}
    }
}