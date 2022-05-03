import java.util.ArrayList;
import java.util.Scanner;

public class Test extends PIDManagerCls{

    static int threadNum;
    static int programLifeTime;
    static int threadLifeTime;
    static int randomCreateTime;
    static long programStartTime;
    static int pidorpidwait;

    public static void main(String[] args){
        Scanner sc = new Scanner(System.in);
        while(true){
            try{
                System.out.print("Input the number of threads: ");
                threadNum = sc.nextInt();
                System.out.print("Input the life time of the program(sec): ");
                programLifeTime = sc.nextInt();
                System.out.print("Input the life time of each thread(sec): ");
                threadLifeTime = sc.nextInt();
                System.out.print("Input the random time range that threads are created(0~?sec): ");
                randomCreateTime = sc.nextInt();
                System.out.print("Input the function for testing, 1.getPID 2.getPIDWait : ");
                pidorpidwait = sc.nextInt();
                if(pidorpidwait !=1 && pidorpidwait !=2){
                    System.out.println("Please input the right answer.");
                    continue;
                }
                System.out.println("Test program is initialized with " +threadNum+ " thread and " +programLifeTime+ " seconds, with the life time " +threadLifeTime+ " seconds of each thread, range of creation term is " +randomCreateTime);
                break;
            }catch (Exception e){
                System.out.println("Please input the right answer.");
                sc = new Scanner(System.in);
            }
        }

        ArrayList<Thread> threads = new ArrayList<>();
        programStartTime = System.currentTimeMillis();

        //*********************program starts********************
        for(int i = 0; i<threadNum; i++){
            //program endcheck at first point
            if ((System.currentTimeMillis() - programStartTime) >= programLifeTime*1000) {
                break;
            }
            //sleep time created
            double random = Math.random();
            int sleepTime = (int) (random*randomCreateTime);
            //program endcheck before sleep time
            if((System.currentTimeMillis() - programStartTime +sleepTime*1000) >= programLifeTime*1000) {
                break;
            }
            //sleep
            try {
                Thread.sleep(sleepTime*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //program endcheck after sleep
            if ((System.currentTimeMillis() - programStartTime) >= programLifeTime*1000) {
                break;
            }
            //thread creation
            PIDThread pidthread = new PIDThread(i);
            Thread thread = new Thread(pidthread);
            thread.start();
            PIDManagerCls.pidThreads.add(pidthread);
            threads.add(thread);
            //program endchck after thread creation
            if ((System.currentTimeMillis() - programStartTime) >= programLifeTime*1000) {
                break;
            }
        }

        //program ends
        while(true){
            if ((System.currentTimeMillis() - programStartTime) >= programLifeTime*1000) {
                for (Thread thread : threads) {
                    thread.interrupt();
                }
                System.out.println(programLifeTime + " seconds has passed... Program ends");
                break;
            }
        }
        //Waiting for result ...
        try{
            Thread.sleep(500);
        }catch (InterruptedException e){
        }

        //print result of pid list
        System.out.println("\n**********The Number of PID used in this process**********");
        for(PIDThread pidThread : PIDManagerCls.pidThreads){
            System.out.println("Thread: "+ pidThread.seq+ " || PID: " +pidThread.id);
        }
    }
}


class PIDThread implements Runnable{
    int seq;
    int id;
    PIDManagerCls pidmanager = new PIDManagerCls();

    public PIDThread(int seq){
        this.seq = seq;
    }

    @Override
    public void run() {
        //id get
        if(Test.pidorpidwait == 2){
            this.id = pidmanager.getPIDWait();
        }else{
            this.id = pidmanager.getPID();
        }
        //thread run
        long threadStartTime = System.currentTimeMillis();
        System.out.println("Thread " +seq+" created at Second "+(threadStartTime - Test.programStartTime)/1000);
        try {
            Thread.sleep((Test.threadLifeTime)*1000);  // 1sec = 1000millis
        } catch (Exception e) {
        }
        System.out.println("Thread " +seq+ " destroyed at Second "+(System.currentTimeMillis()-Test.programStartTime)/1000);
        //release pid
        if(Test.pidorpidwait == 2){
            pidmanager.releasePID(this.id);
        }
    }
}


class PIDManagerCls implements PIDManager {

    static ArrayList<PIDThread> pidThreads = new ArrayList<>();
    static int[] pidList = new int[(MAX_PID + 1)]; //기본값 = 0

    @Override
    public synchronized int getPID() {
        for (int i = MIN_PID; i <= MAX_PID; i++) {
            if (pidList[i] == 0) {
                pidList[i] = 1;
                return i;
            }
        }
        //만약 가능한 id가 없을 경우
        return -1;
    }

    @Override
    public synchronized int getPIDWait() {
        int pid;
        do {
            pid = getPID();
            if (pid == -1) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } while (pid == -1);
        return pid;
    }

    @Override
    public synchronized void releasePID(int pid) {
        pidList[pid] = 0;
        notify();
    }
}
