package test;
public class runnabletest implements Runnable{
 
    private int j;
    private int tickets;//��Դ���������߳���˵�ǹ���ġ�
    public runnabletest(int ticketNum) {
    	tickets = ticketNum;
    }
    @Override
    public void run() {
      for (int i=0;i<=20;i++) {//һ�����   ����Դ����%�߳�����+1�� ��ȷ������֤��Դ�õ�����
        
    	//����߳���ʵ�� runnable �ӿڻ�ȡ��ǰ���̣߳�ֻ���� Thread.currentThread() ��ȡ��ǰ���߳���
        Thread.currentThread().getName();
        
        if(tickets>0){
        	System.out.println(Thread.currentThread().getName()+"--����Ʊ��" + tickets--+"��Ʊ");
        	System.out.println(j++);
        }
      }
    }
    
    public static void main(String[] args){
     
        runnabletest xxx = new runnabletest(10);
     
        //ͨ��new Thread(target,name)�����µ��߳�
        new Thread(xxx,"��Ʊ��1").start();
        new Thread(xxx,"��Ʊ��2").start();
        new Thread(xxx,"��Ʊ��3").start();
        
        System.out.println(xxx.j);
      }
}
