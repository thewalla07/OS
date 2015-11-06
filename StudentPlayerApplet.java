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
<<<<<<< HEAD
    private Thread pthread;
    private Thread cthread;
=======
	
	//this block moved from run - defined them here so can use them in all methods
		AudioInputStream s;
        AudioFormat format;
		DataLine.Info info;
		int oneSecond;
		BoundedBuffer b;
		SourceDataLine line;
		Thread pthread;
        Thread cthread;
>>>>>>> refs/remotes/origin/master

    public Player(String filename){

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
<<<<<<< HEAD
                    textarea.append("You said: " + e.getActionCommand() + "\n");
                    textfield.setText("");
                    if(e.getActionCommand().equals("x")){
                        cthread.Consumer.stopConsumer();
                    }
=======
					String input = e.getActionCommand();
					switch(input){
						case "x":
							try{
								//System.out.println("attempted stop");
								
								pthread.sleep(5);
								cthread.sleep(5);
								
								line.stop();
								line.close();
								
								//System.out.println("reached end");
							}
							catch(InterruptedException v){
								Thread.currentThread().interrupt();
							}
							break;
							
						case "q":
							//raise volume
							break;
							
						case "a":
							//lower volume 
							break;
							
						case "p":
							//pause playback
							break;
							
						case "r":
							//resume playback
							break;
							
						case "m":
							//mute 
							break;
							
						case "u":
							//unmute
							break;
							
						default:
							//default - do nothing wrong input 
							break;
						
					}
>>>>>>> refs/remotes/origin/master
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
		
            System.out.println("Audio format: " + format.toString());
            info = new DataLine.Info(SourceDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                throw new UnsupportedAudioFileException();
            }

            oneSecond = (int) (format.getChannels() * format.getSampleRate() * format.getSampleSizeInBits()/8 );

            b = new BoundedBuffer(oneSecond);

            line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(format, oneSecond);
            line.start();
            
            pthread = new Thread(new Producer(oneSecond, s, b));
			cthread = new Thread(new Consumer(oneSecond, line, b));

            pthread.start();           
            cthread.start();

            pthread.join();
            System.out.println("Pclosed");

            cthread.join();
			System.out.println("Cclosed");
            
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
        } catch(InterruptedException e){
            System.out.println("IE");
            e.printStackTrace();
            System.exit(1);
        }
    }
}

class Consumer implements Runnable{
    byte[] audioChunk;
    int oneSecond, bytesRead;
    SourceDataLine line;
    BoundedBuffer b;
    private volatile boolean stop = false;

    Consumer(int oneSecond0, SourceDataLine line0, BoundedBuffer b0){
        oneSecond=oneSecond0;
        line=line0;
        b=b0;
        audioChunk= new byte[oneSecond];
        bytesRead=0;
    }

    public void stopConsumer(){
        stop=true;
    }

    public void run(){
        try{
            while(!b.isDone()&&!stop){
                audioChunk=b.removeChunk();
                line.write(audioChunk, 0, audioChunk.length);
            }
            System.out.println("Bye from Consumer");
        } catch(Exception e){
            return;
        }
    }
}

class Producer implements Runnable{
    byte[] audioChunk;
    int oneSecond, bytesRead;
    AudioInputStream s;
    BoundedBuffer b;

    Producer(int oneSecond0, AudioInputStream s0, BoundedBuffer b0){
        oneSecond=oneSecond0;
        s=s0;
        b=b0;
        audioChunk= new byte[oneSecond];
        bytesRead=0;
    }

    public void run(){
        try{
            while(bytesRead!=-1){
                bytesRead=s.read(audioChunk);
                //System.out.println(bytesRead);
                b.insertChunk(audioChunk);
            }
            b.done();
            System.out.println("Bye from Producer");
        } catch(IOException e){
            e.printStackTrace();
            System.out.println("Producer interrupted");
        }
    }
}

class BoundedBuffer{
    byte[] bufferArray;
    byte[] transfer;
    int nextIn, nextOut, amountOccupied, chunkSize, i, j;
    boolean isFull, isEmpty, isDone;

    BoundedBuffer(int chunkSize0){
        bufferArray=new byte[10*chunkSize0];
        nextIn=0;
        nextOut=0;
        amountOccupied=0;
        chunkSize=chunkSize0;
        isFull=true;
        isEmpty=true;
        isDone=false;
        transfer=new byte[chunkSize];
    }

    public synchronized void done(){
        while(amountOccupied>0)try{wait();}catch(InterruptedException e){}
        isDone=true;
        notifyAll();
    }

    public boolean isDone(){return isDone;}

    public synchronized void insertChunk(byte[] input){
        try{
            while(amountOccupied==10){
                wait();
            }
            for(int i=0; i < input.length; i++){
                bufferArray[(nextIn+i)%(chunkSize*10)]=input[i];
            }
            nextIn+=input.length%(chunkSize*10);
            amountOccupied++;
            notifyAll();
        } catch(InterruptedException e){
            e.printStackTrace();
            System.out.println("Insert chunk failed");
        } catch(ArrayIndexOutOfBoundsException e){
            e.printStackTrace();
        }
    }

    public synchronized byte[] removeChunk(){
        try{
            while(amountOccupied==0){
                wait();
            }
            for(int j=0; j < transfer.length; j++){
                transfer[j]=bufferArray[(nextOut+j)%(chunkSize*10)];
            }
            nextOut+=transfer.length%(chunkSize*10);
            amountOccupied--;
            notifyAll();
            return transfer;
        } catch(InterruptedException e){
            e.printStackTrace();
            System.out.println("Remove chunk failed");
            return null;
        } catch(ArrayIndexOutOfBoundsException e){
            e.printStackTrace();
            return null;
        }
    }
}