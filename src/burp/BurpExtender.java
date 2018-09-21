package burp;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;


public class BurpExtender implements IBurpExtender, ITab, IContextMenuFactory
{
    public IBurpExtenderCallbacks callbacks;
    private IExtensionHelpers helpers;
    
    private PrintWriter stdout;//�������ﶨ�����������registerExtenderCallbacks������ʵ������������ں����о�ֻ�Ǿֲ���������������ʵ��������ΪҪ�õ�����������
    private PrintWriter stderr;
    private String ExtenderName = "Domain Hunter v1.0 by bit4";
    private String github = "https://github.com/bit4woo/domain_hunter";
    private String summary = "      Sub-domain:%s  Similar-domain:%s  Related-domain:%s  ^_^";
    private Set<String> subdomainofset = new HashSet<String>();
    private Set<String> domainlikeset = new HashSet<String>();
    private Set<String> relatedDomainSet = new HashSet<String>();

	private JPanel contentPane;
	private JTextField textFieldSubdomains;
	private JTextField textFieldDomainsLike;
	private JLabel lblSubDomainsOf;
	private JButton btnSearch;
	private JLabel lblSummary;
	private JPanel panel_2;
	private JLabel lblNewLabel_2;
	private JSplitPane splitPane;
	private Component verticalStrut;
	private JTextArea textArea;
	private JTextArea textArea_1;
	private JButton btnNewButton;
	private JTextArea textArea_2;
    
    @Override
    public void registerExtenderCallbacks(IBurpExtenderCallbacks callbacks)
    {
    	stdout = new PrintWriter(callbacks.getStdout(), true);
    	stderr = new PrintWriter(callbacks.getStderr(), true);
    	stdout.println(ExtenderName);
    	stdout.println(github);
        this.callbacks = callbacks;
        helpers = callbacks.getHelpers();
        callbacks.setExtensionName(ExtenderName); //�������
        callbacks.registerContextMenuFactory(this);
        addMenuTab();
        
    }

	public Map<String, Set<String>> search(String subdomainof, String domainlike){
			subdomainofset.clear();
			domainlikeset.clear();
			relatedDomainSet.clear();
			
			
			if (subdomainof.contains(".")&&!subdomainof.endsWith(".")&&!subdomainof.startsWith("."))
			{
				Set<String> httpsURLs = new HashSet<String>();
				IHttpRequestResponse[] requestResponses = callbacks.getSiteMap(null);
				    //stdout.println(response[1]);
			    for (IHttpRequestResponse x:requestResponses){
			    	
			    	IHttpService httpservice = x.getHttpService();
			    	String shortURL = httpservice.toString();
			    	String protocol =  httpservice.getProtocol();
					String Host = httpservice.getHost();
					
					//stdout.println(shortURL);
					//stdout.println(Host);
					
					if (Host.endsWith("."+subdomainof)){
						subdomainofset.add(Host);
						//stdout.println(subdomainofset);
						//get SANs info to get related domain, only when the [subdomain] is using https.
						if(protocol.equalsIgnoreCase("https")) {
								httpsURLs.add(shortURL);
						}
						continue;
					}
					
					
					if (!domainlike.equals("") && Host.contains(domainlike) && !Host.equalsIgnoreCase(subdomainof)){
						domainlikeset.add(Host);
						if(protocol.equalsIgnoreCase("https")) {
							httpsURLs.add(shortURL);
						}
						//stdout.println(domainlikeset);
						continue;
					}
					
					if (validIP(Host)) {//https://202.77.129.30
						if(protocol.equalsIgnoreCase("https")) {
							httpsURLs.add(shortURL);
						}
					}
			    }
				    
			    stdout.println("sub-domains and similar-domains search finished,starting get related-domains");
			    //stdout.println(httpsURLs);
			    
			    //���̻߳�ȡ
			    //Set<Future<Set<String>>> set = new HashSet<Future<Set<String>>>();
		    	Map<String,Future<Set<String>>> urlResultmap = new HashMap<String,Future<Set<String>>>();
		        ExecutorService pool = Executors.newFixedThreadPool(10);
		        
		        for (String url:httpsURLs) {
		          Callable<Set<String>> callable = new ThreadCertInfo(url,domainlike);
		          Future<Set<String>> future = pool.submit(callable);
		          //set.add(future);
		          urlResultmap.put(url, future);
		        }
		        
		        Set<String> tmpRelatedDomainSet = new HashSet<String>();
		        for(String url:urlResultmap.keySet()) {
		        	Future<Set<String>> future = urlResultmap.get(url);
		        //for (Future<Set<String>> future : set) {
		          try {
		        	  stdout.println("founded related-domains :"+future.get() +" from "+url);
		        	  if (future.get()!=null) {
		        		  tmpRelatedDomainSet.addAll(future.get());
		        	  }
		        	  
				} catch (Exception e) {
					//e.printStackTrace(stderr);
					stderr.println(e.getMessage());
		        }
		        }
			    
		        /* ���̻߳�ȡ��ʽ
			    Set<String> tmpRelatedDomainSet = new HashSet<String>();
			    //begin get related domains
			    for(String url:httpsURLs) {
			    	try {
			    		tmpRelatedDomainSet.addAll(CertInfo.getSANs(url));
					}catch(UnknownHostException e) {
						stderr.println("UnknownHost "+ url);
						continue;
					}catch(ConnectException e) {
						stderr.println("Connect Failed "+ url);
						continue;
					}
			    	catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace(stderr);
						continue;
					}
			    }
			    */
			    
			    //�� SANs�Ľ������һ�η��ࡣ
			    for (String item:tmpRelatedDomainSet) {
			    	if (item.contains(".")&&!item.endsWith(".")&&!item.startsWith(".")) {
				    	if (item.endsWith("."+subdomainof)){
							subdomainofset.add(item);
						}else if (!domainlike.equals("") && item.contains(domainlike) && !item.equalsIgnoreCase(subdomainof)){
							domainlikeset.add(item);
						}else {
							relatedDomainSet.add(item);
						}
			    	}
			    }

			}
		    
		    Map<String, Set<String>> result = new HashMap<String, Set<String>>();
		    result.put("subdomainofset", subdomainofset);
		    result.put("domainlikeset", domainlikeset);
		    result.put("relatedDomainSet", relatedDomainSet);
		    return result;
		    
	}
 
	public Map<String, Set<String>> spiderall (String subdomainof, String domainlike) {
		
		if (!subdomainof.contains(".")||subdomainof.endsWith(".")||subdomainof.equals("")){
			//�������Ϊ�գ����ߣ�������.�ţ����ߵ����ĩβ�ģ�
		}
		else {
		    int i = 0;
		    while(i<=2) {
		    	IHttpRequestResponse[] items = callbacks.getSiteMap(null); //null to return entire sitemap
		    	//int len = items.length;
		    	//stdout.println("item number: "+len);
		    	Set<URL> NeedToCrawl = new HashSet<URL>();
			    for (IHttpRequestResponse x:items){// ������֤ÿ�ζ���Ҫ��ͷ��ʼ��������һ��offset��ȡ������ÿ�ζ����ܲ�ͬ
			    	IRequestInfo  analyzeRequest = helpers.analyzeRequest(x); //ǰ��Ĳ��������Ѿ��޸���������������������ĸ���ǰ������Ҫ���»�ȡ��
					
					String Host = analyzeRequest.getUrl().getHost();
					
					URL shortUrl=analyzeRequest.getUrl();
					try {
						String urlstring = x.getHttpService().toString();
						shortUrl = new URL(urlstring);
					} catch (MalformedURLException e) {
						e.printStackTrace(stdout);
					}

					if (Host.endsWith("."+subdomainof) && isResponseNull(x)) {
						// to reduce memory usage, use isResponseNull() method to adjust whether the item crawled.
						NeedToCrawl.add(shortUrl);
						// to reduce memory usage, use shortUrl. base on my test, spider will crawl entire site when send short URL to it.
						// this may miss some single page, but the single page often useless for domain collection
						// see spideralltest() function.
					}
				}
			    
			    
				for (URL shortUrl:NeedToCrawl) {
					if (!callbacks.isInScope(shortUrl)) { //reduce add scope action, to reduce the burp UI action.
						callbacks.includeInScope(shortUrl);//if not, will always show confirm message box.
					}
					callbacks.sendToSpider(shortUrl);
				}

			    
				try {
					Thread.sleep(5*60*1000);//��λ���룬60000����=һ����
					stdout.println("sleep 5 minutes to wait spider");
					//to wait spider
				} catch (InterruptedException e) {
					e.printStackTrace(stdout);
				}
		    	i++;
			}
		}
		
	    return search(subdomainof,domainlike);
	    //search(subdomainof,domainlike);
	    }
	
	
	public Map<String, Set<String>> spideralltest (String subdomainof, String domainlike) {
		
		int i = 0;
		while (i<=10) {
			try {
				callbacks.sendToSpider(new URL("http://www.baidu.com/"));
				Thread.sleep(1*60*1000);//��λ���룬60000����=һ����
				stdout.println("sleep 1 min");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			i++;
			// to reduce memory usage, use isResponseNull() method to adjust whether the item crawled.
		}
		
		Map<String, Set<String>> result = new HashMap<String, Set<String>>();
		return result;
	}
	public String set2string(Set set){
	    Iterator iter = set.iterator();
	    String result = "";
	    while(iter.hasNext())
	    {
	        //System.out.println(iter.next());  	
	    	result +=iter.next();
	    	result +="\n";
	    }
	    return result;
	}
	
	public boolean isResponseNull(IHttpRequestResponse message){
		try {
			int x = message.getResponse().length;
			return false;
		}catch(Exception e){
			//stdout.println(e);
			return true;
		}
	}
	
	public Boolean uselessExtension(String urlpath) {
		Set<String> extendset = new HashSet<String>();
		extendset.add(".gif");
		extendset.add(".jpg");
		extendset.add(".png");
		extendset.add(".css");
		Iterator<String> iter = extendset.iterator();
		while (iter.hasNext()) {
			if(urlpath.endsWith(iter.next().toString())) {//if no next(), this loop will not break out
				return true;
			}
		}
		return false;
	}
		
	public void CGUI() {
			contentPane =  new JPanel();
			contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
			contentPane.setLayout(new BorderLayout(0, 0));
	
			
			JPanel panel = new JPanel();
			FlowLayout flowLayout_1 = (FlowLayout) panel.getLayout();
			flowLayout_1.setAlignment(FlowLayout.LEFT);
			contentPane.add(panel, BorderLayout.NORTH);
			
			lblSubDomainsOf = new JLabel("SubDomains of  ");
			panel.add(lblSubDomainsOf);
			
			textFieldSubdomains = new JTextField();
			textFieldSubdomains.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent arg0) {
					String domain = textFieldSubdomains.getText();
					textFieldDomainsLike.setText(domain.substring(0,domain.lastIndexOf(".")));
				}
			});
			panel.add(textFieldSubdomains);
			textFieldSubdomains.setColumns(20);
			
			verticalStrut = Box.createVerticalStrut(20);
			panel.add(verticalStrut);
			
			JLabel lblDomainsLike = new JLabel("Domains like ");
			panel.add(lblDomainsLike);
			
			textFieldDomainsLike = new JTextField();
			panel.add(textFieldDomainsLike);
			textFieldDomainsLike.setColumns(20);
			
			btnSearch = new JButton("Search");
			btnSearch.setToolTipText("Do a single search from site map");
			btnSearch.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
				    	//using SwingWorker to prevent blocking burp main UI.

				        @Override
				        protected Map doInBackground() throws Exception {                
							String subdomain = textFieldSubdomains.getText();
							String domainlike = textFieldDomainsLike.getText();
							btnSearch.setEnabled(false);
							return search(subdomain,domainlike);
				        }
				        @Override
				        protected void done() {
				            try {
					        	Map result = get();
					        	subdomainofset = (Set) result.get("subdomainofset"); //֮ǰ��set�����object
					        	domainlikeset = (Set) result.get("domainlikeset");
					        	relatedDomainSet = (Set) result.get("relatedDomainSet");
								textArea.setText(set2string(subdomainofset));
								textArea_1.setText(set2string(domainlikeset));
								textArea_2.setText(set2string(relatedDomainSet));
								btnSearch.setEnabled(true);
								lblSummary.setText(String.format(summary, subdomainofset.size(),domainlikeset.size(),relatedDomainSet.size()));
				            } catch (Exception e) {
				            	btnSearch.setEnabled(true);
				                e.printStackTrace(stderr);
				            }
				        }
				    };      
				    worker.execute();
					
				}
			});
			panel.add(btnSearch);
			
			btnNewButton = new JButton("Spider All");
			btnNewButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					
				    SwingWorker<Map, Map> worker = new SwingWorker<Map, Map>() {
				    	//������һ������ʵ����һ���ֱ࣬��ʵ��ԭʼ�࣬û�б�����������ţ�
				    	//֮ǰ���뷨���ȵ���ʵ��һ��worker�࣬�������洦����֣��Ͷ���һ��ʵ�֣�Ȼ����������ã��������û���һ�������⡣
				    	//https://stackoverflow.com/questions/19708646/how-to-update-swing-ui-while-actionlistener-is-in-progress
				        @Override
				        protected Map doInBackground() throws Exception {                
							String subdomain = textFieldSubdomains.getText();
							String domainlike = textFieldDomainsLike.getText();
							//stdout.println(subdomain);
							//stdout.println(domainlike);
							btnNewButton.setEnabled(false);
							return spiderall(subdomain,domainlike);
						
				        }
				        @Override
				        protected void done() {
				            try {
					        	Map result = get();
					        	subdomainofset = (Set<String>) result.get("subdomainofset"); //֮ǰ��set�����object
					        	domainlikeset = (Set<String>) result.get("domainlikeset");
								textArea.setText(set2string(subdomainofset));
								textArea_1.setText(set2string(domainlikeset));
								btnNewButton.setEnabled(true);
				            } catch (Exception e) {
				                e.printStackTrace();
				            }
				        }
				    };      
				    worker.execute();
				}
			});
			btnNewButton.setToolTipText("Spider all subdomains recursively,This may take a long time!!!");
			panel.add(btnNewButton);
			
			lblSummary = new JLabel("      ^_^");
			panel.add(lblSummary);
			
			splitPane = new JSplitPane();
			splitPane.setDividerLocation(0.5);
			contentPane.add(splitPane, BorderLayout.WEST);
			
			textArea = new JTextArea();
			textArea.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent arg0) {
					final JPopupMenu jp = new JPopupMenu();
			        jp.add("^_^");
			        textArea.addMouseListener(new MouseAdapter() {
			            @Override
			            public void mouseClicked(MouseEvent e) {
			                if (e.getButton() == MouseEvent.BUTTON3) {
			                    // �����˵�
			                    jp.show(textArea, e.getX(), e.getY());
			                }
			            }
			        });
				}
			});
			textArea.setColumns(30);
			splitPane.setLeftComponent(textArea);
			
			textArea_1 = new JTextArea();
			textArea_1.setColumns(30);
			splitPane.setRightComponent(textArea_1);
			
			JSplitPane splitPane_1 = new JSplitPane();
			splitPane.setDividerLocation(0.5);
			contentPane.add(splitPane_1, BorderLayout.EAST);
			
			textArea_2 = new JTextArea();
			textArea_2.setColumns(30);
			splitPane_1.setLeftComponent(textArea_2);
			
			panel_2 = new JPanel();
			FlowLayout flowLayout = (FlowLayout) panel_2.getLayout();
			flowLayout.setAlignment(FlowLayout.LEFT);
			contentPane.add(panel_2, BorderLayout.SOUTH);
			
			lblNewLabel_2 = new JLabel(ExtenderName+"    "+github);
			lblNewLabel_2.setFont(new Font("����", Font.BOLD, 12));
			lblNewLabel_2.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					try {
						URI uri = new URI(github);
						Desktop desktop = Desktop.getDesktop();
						if(Desktop.isDesktopSupported()&&desktop.isSupported(Desktop.Action.BROWSE)){
							desktop.browse(uri);
						}
					} catch (Exception e2) {
						// TODO: handle exception
						BurpExtender.this.callbacks.printError(e2.getMessage());
					}
					
				}
				@Override
				public void mouseEntered(MouseEvent e) {
					lblNewLabel_2.setForeground(Color.BLUE);
				}
				@Override
				public void mouseExited(MouseEvent e) {
					lblNewLabel_2.setForeground(Color.BLACK);
				}
			});
			panel_2.add(lblNewLabel_2);
	}
		
//�����Ǹ���burp����ķ��� --start
    
    public void addMenuTab()
    {
      SwingUtilities.invokeLater(new Runnable()
      {
        public void run()
        {
          BurpExtender.this.CGUI();
          BurpExtender.this.callbacks.addSuiteTab(BurpExtender.this); //�����BurpExtender.thisʵ����ָITab����Ҳ����getUiComponent()�е�contentPane.���������CGUI()������ʼ����
          //������ﱨjava.lang.NullPointerException: Component cannot be null ������Ҫ�Ų�contentPane�ĳ�ʼ���Ƿ���ȷ��
        }
      });
    }
    
    
    
    //ITab����ʵ�ֵ���������
	@Override
	public String getTabCaption() {
		// TODO Auto-generated method stub
		return ("Domain Hunter");
	}
	@Override
	public Component getUiComponent() {
		// TODO Auto-generated method stub
		return this.contentPane;
	}
	//ITab����ʵ�ֵ���������

	@Override
	public List<JMenuItem> createMenuItems(IContextMenuInvocation invocation) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	//IContextMenuFactory ����ʵ�ֵķ���
	//����burp����ķ��� --end
	
	
	public static boolean validIP (String ip) {
	    try {
	        if ( ip == null || ip.isEmpty() ) {
	            return false;
	        }

	        String[] parts = ip.split( "\\." );
	        if ( parts.length != 4 ) {
	            return false;
	        }

	        for ( String s : parts ) {
	            int i = Integer.parseInt( s );
	            if ( (i < 0) || (i > 255) ) {
	                return false;
	            }
	        }
	        if ( ip.endsWith(".") ) {
	            return false;
	        }

	        return true;
	    } catch (NumberFormatException nfe) {
	        return false;
	    }
	}
	
	public static void main(String args[]) {
		System.out.println("aaa".contains(""));
	}
}