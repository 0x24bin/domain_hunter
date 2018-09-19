package burp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ThreadCertInfo implements Callable<Set<String>>{
	/*
    private Set<String> urls;
    public ThreadCertInfo(Set<String> urls) {
    	this.urls = urls;
    }
    
    
    @Override
    public Set<String> call(){
    	Set<String> tmpDomains = new HashSet<String>();
      for (int i=0;i<=urls.size()/10+1;i++) {//һ�����   ����Դ����%�߳�����+1�� ��ȷ������֤��Դ�õ�����
        
    	//����߳���ʵ�� runnable �ӿڻ�ȡ��ǰ���̣߳�ֻ���� Thread.currentThread() ��ȡ��ǰ���߳���
        Thread.currentThread().getName();
        
        if(urls.iterator().hasNext()){
        	try {
				tmpDomains = CertInfo.getSANs(urls.iterator().next());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
      }
	return null;
    }
	*/
    
	private String url;
	private String domain;
    public ThreadCertInfo(String url,String domain) {
    	this.url = url;
    	this.domain = domain;
    }
    
    
    @Override
    public Set<String> call() throws Exception{
		Set<String> tmpDomains = CertInfo.getSANs(url,domain);
		return tmpDomains;
    }
	
    public static void main(String[] args) {

    	Set<String> urls = new HashSet<String>();
    	urls.add("https://202.77.129.30");
    	urls.add("https://ebppweb.alipay.com");
    	urls.add("https://opendoc.cloud.alipay.com");
    	urls.add("https://ab.alipay.com");
    	urls.add("https://goldetfprod.alipay.com");
    	urls.add("https://tfs.alipay.com");
    	urls.add("https://docs.alipay.com");
    	urls.add("https://emembercenter.alipay.com");
    	urls.add("https://docs.open.alipay.com");
    	urls.add("https://benefitprod.alipay.com");
    	urls.add("https://mapi.alipay.com");
    	urls.add("https://ie.alipay.com");
    	urls.add("https://fun.alipay.com");
    	urls.add("https://shenghuo.alipay.com");
    	urls.add("https://home.alipay.com");
    	
    	Set<Future<Set<String>>> set = new HashSet<Future<Set<String>>>();
    	Map<String,Future<Set<String>>> urlResultmap = new HashMap<String,Future<Set<String>>>();
        ExecutorService pool = Executors.newFixedThreadPool(3);
        
        for (String word: urls) {

          Callable<Set<String>> callable = new ThreadCertInfo(word,"alipay.com");
          Future<Set<String>> future = pool.submit(callable);
          set.add(future);
          urlResultmap.put(word, future);
        }
        
        
        
        Set<String> Domains = new HashSet<String>();
        for(String url:urlResultmap.keySet()) {
        	Future<Set<String>> future = urlResultmap.get(url);
        //for (Future<Set<String>> future : set) {
          try {
        	  System.out.println(url);
        	  System.out.println(future.get());
        	  if (future.get()!=null) {
        		  Domains.addAll(future.get());
        	  }
        	  
		} catch (InterruptedException e) {
			System.out.println(e.getMessage());
		} catch (ExecutionException e) {
			System.out.println(e.getMessage());
		}catch(Exception e) {
			System.out.println(e.getMessage());
		}
        }
        
        System.out.println(set2string(Domains));
      }

    
    
	public static String set2string(Set set){
	    Iterator<?> iter = set.iterator();
	    String result = "";
	    while(iter.hasNext())
	    {
	        //System.out.println(iter.next());  	
	    	result +=iter.next();
	    	result +="\n";
	    }
	    return result;
	}
}
