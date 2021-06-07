package ClientSideMiddle;

import java.io.FileReader;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.Semaphore;

import org.json.JSONException;
import org.json.JSONObject;

public class MiddleWare implements NetworkerUser
{
	private static final String CHARSET = "UTF-8";
	private static final int COMM = 100;
	private static final String CONFIGFILE = "./ClientSideMiddle/config.txt";
	private static final String IPPORT = "./ClientSideMiddle/ipPort.txt";
	private static final String CAR = "CAR";
	private static final String LICENSE_PLATE = "LICENSE_PLATE";
	private static final String MAKE = "MAKE";
	private static final String YEAR = "YEAR";
	private static final String IP_ADDRESS = "IP_ADDRESS";
	private static final String PORT_NUMB = "PORT_NUMB";
	private static final char SEP_FIELD = ':';
	private static final char NEXT_LINE = '\n';
	private static final String ID = "ID";
	private static final String PROTOCOL_END = "\r\n";
	
	public String id;
	private String licensePlate;
	private String car;
	private String make;
	private int year;
	private String ipAddr;
	private int portNumb;
	private Networker networker;
	private LinkedListQueue<CarEvent> eventQueue;
	private Mutex eventQueueMut;
	
	public MiddleWare() 
	{
		eventQueueMut = new Mutex(1);
		
		try {
			File in = new File(CONFIGFILE);
			if(in.createNewFile())
			{
				System.out.println("cofig.txt File created ");
			}
			else
			{
				System.out.println("config.txt already exist!");
				getInfoFromConfig();
				id = licensePlate.toUpperCase() + car.toUpperCase() + make.toUpperCase() + year; // 다른방법으로 id를만들고 싶으시면 이 코드를 삭제하거나 바꾸시면 됩니다. 
																								// 밑에있는 주석도 읽어서 바꾸시고 싶은 부분을 바꿔주세요.
			}
		}
		
		catch(IOException ex)
		{
			System.out.println("An error occurred in creating MiddleWare while opening config.txt");
			ex.printStackTrace();
		}
		
		try 
		{
			File in = new File(IPPORT);
			if(in.createNewFile())
			{
				System.out.println("ipPort.txt File created");
			}
			else
			{
				System.out.println("ipPort.txt File already exist!");
				getInfoFromIpPort();
			}
		}
		catch(IOException ex)
		{
			System.out.println("An error occurred in createing Middleware while opening IpPort.txt");
		}
		
		eventQueue = new LinkedListQueue<CarEvent>();
		Socket conn = null;
	}
	
	public boolean run() // ip주소와 포트넘버를 체크하고 네트워커 객채를 생성합니다.
	{
		if(ipAddr == null || portNumb == 0)
		{
			System.err.println("invalid port and Ip! ");
			return false;
		}
		
		try 
		{
			
			Socket sock = new Socket(ipAddr, portNumb);
			networker = new Networker(sock, this, PROTOCOL_END);
			

		}
		catch(IOException ex)
		{
			System.err.println("Failed to create Socket!");
			System.err.println(ex);
		}
		
		
				return true;
	}
	
	
	public void close()
	{
		networker.close();
	}
	
	
	private boolean getInfoFromIpPort() //ipPort.txt에서 정보를 읽습니다. 포멧은 fieldName:fieldValue\n 입니다. ipPort.txt는 미리 값을 넣어야 됩니다. 
	{
		return getInfoFromFile(IPPORT);
	}
	
	private boolean getInfoFromConfig()//config.txt에서 정보를 읽습니다. 포멧은 fieldName:fieldValue\n 입니다. 
	{
		return getInfoFromFile(CONFIGFILE);
	}
	
	private boolean getInfoFromFile(String fileName) //해당 파일을 읽습니다. 
	{
		
		StringBuilder strBuilder = new StringBuilder();
		String fieldName = null;
		String fieldValue = fieldName;
		int tempChar;
		
		try 
		{
			Reader input = new BufferedReader(new FileReader(new File(fileName)));
			
			while((tempChar = input.read()) !=  -1)
			{
				if((char) tempChar == SEP_FIELD)  // ':' 를 만날때까지 읽습니다. ':'를 만났다면 필드이름(예를 들어서 licensePlate)을 다읽었다는 것을 의미합니다.
				{
					fieldName = strBuilder.toString();
					strBuilder.setLength(0);
				}
				else if((char) tempChar == NEXT_LINE) // '\n'을 만날때까지 읽습니다. '\n'을 만나면 필드의 값을 다 읽었다는 뜻입니다. 
				{
					fieldValue = strBuilder.toString();
					putValue(fieldName, fieldValue);  // 필드 이름과 읽은 필드의 값을 미들웨어의 변수에 넣습니다.
					strBuilder.setLength(0);
				}
				else
				{
					strBuilder.append((char) tempChar);
				}
			}
			
			fieldValue = strBuilder.toString();
			if(!fieldValue.equals(""))
			{
				putValue(fieldName, fieldValue);
			}
			
			return true;
		}
		catch(IOException ex)
		{
			System.err.println("error occurred in getInfoFromConfig() \n");
			System.err.println(ex);
			return false;
		}
		
	}
	
	private void putValue(String fieldName, String fieldValue) // 이 클래스의 필드(instance 변수)의 값들을 변경합니다. 
    {														   // 필드의 이름은 fieldName이고, fieldValue는 그 필드의 값입니다.
		if(fieldName == null || fieldValue == null)            // 다른 field를 사용하고 싶으시다면 이 코드를 변경하시면 됩니다.
		{
			return;
		}
		if(fieldName.equals(LICENSE_PLATE))
		{
			licensePlate = fieldValue;
			System.out.println("LicensePlate initialized!: " + licensePlate);
		}
		else if(fieldName.equals(CAR))
		{
			car = fieldValue;
			System.out.println("Car initialized: " + car);
		}
		else if(fieldName.equals(MAKE))
		{
			make = fieldValue;
			System.out.println("Make initialized: " + make);
		}
		
		else if(fieldName.equals(YEAR))
		{
			try {
		         year = Integer.parseInt( fieldValue );
		         System.out.println("Year initialized: " + year);
		        
		    }
		    catch(NumberFormatException e ) 
			{
		    	System.err.println("cannot convert YEAR value to integer: erro in putValue()");
		    	System.err.println(e);
		    }
		}
		else if(fieldName.equals(IP_ADDRESS))
		{
			if(checkIpFormat(fieldValue))
			{
				ipAddr = fieldValue;
				System.out.println("ipAddr initialized: " + ipAddr);
			}
			else
			{
				System.err.println("unapprorpiate ipaddress in file!");
			}
			
		
			
		}
		else if(fieldName.equals(PORT_NUMB))
		{
			try
			{
				portNumb = Integer.parseInt(fieldValue);
				System.out.println("portNumb initialized: " + portNumb);
			}
			catch(NumberFormatException e)
			{
				System.err.println("cannot convert portNumb! error in putValue()");
				System.err.println(e);
			}
		}
	}

	public boolean config(String licensePlate, String car, String make, int year) // config파일에 데이터를 쓰는 코드 입니다. 다른 데이터를 저장하고 싶으면 여기서 변경하세요.
	{
		Path path = Paths.get(CONFIGFILE);
		this.licensePlate = licensePlate;
		this.car = car;
		this.make =make;
		this.year = year;
		id = licensePlate + car + make + year;
		
		try(BufferedWriter output = Files.newBufferedWriter(path, StandardOpenOption.WRITE)) // config 파일에 write합니다. 만약 다른값을 넣고 싶으면 여기서 변경하시면 됩니다. 
		{
			output.write(LICENSE_PLATE + ":" + licensePlate +'\n');
			output.write( CAR + ":" + car + '\n');
			output.write( MAKE + ":" + make + '\n');
			output.write( YEAR + ":" + year + '\n');
			output.flush();
			
		}
		catch(IOException ex)
		{
			System.err.println("Failed to write data in config.txt");
			return false;
		}
		
		return true;
	}
	
	private CarEvent convertToEvent(String msg) // returns null if msg not satisfies the requirements
	{
		try
		{
			String tempStr =msg.substring(0, msg.length() - PROTOCOL_END.length());
			System.out.println(tempStr);
			JSONObject temp = new JSONObject(tempStr);
			if((temp.getJSONObject("HEADER") == null) || (temp.getJSONObject("BODY")) == null)
				return null;
			
			return new CarEvent(temp);
		}
		catch(JSONException e)
		{
			System.err.println("error occurred during converToEvent(): " + msg);
			System.err.println(e);
		}
		return null;
	}
	
	public void send(CarEvent eve) // does not send if id is null!, id가 없을때 보내지 않습니다. 메세지를 버려버립니다. 만약 다른 행동을 원하면, 이 코드를 변경하세요.
	{
		if(eve == null)
			return;
		
		
		if(eve.getHeader(ID) == null)
		{
			if(id == null)
				return;
			eve.putHeader(ID, id);
		}
		
		networker.send(converToString(eve));
			
	}
	
	//연결이 끊겼을때 호출되는 함수입니다. 지금은 아무것도 하지않습니다. 이 코드를 변경해서 못 보낸 event들을 다르게 처리하도록 하세요.
	public void onTearedOff(String networkerId, List<String> unSentEvent, Networker networker)
	{
		System.out.println("networkId: " + networkerId);
		for(String msg: unSentEvent)
		{
			System.out.println(msg);
		}
		
		networker = null;
	}
	
	public void onRecieve(String msg , Networker networker)
	{
		CarEvent temp = convertToEvent(msg);
		System.out.println("got msg");
		if(temp != null)
		{
			String eventNo;
			if((eventNo = temp.getHeader("EVENTNO")) != null)
			{
				try {
					if(Integer.parseInt(eventNo) == 0)
					{
						serverRedirect(temp.getBody("IPADDR"), Integer.parseInt(temp.getBody("PORTNUM")));
						
					}
					else
					{
						try
						{
						eventQueueMut.acquire();
						eventQueue.add(temp);
						}
						catch(InterruptedException e)
						{
							System.err.println("error in putting msg: " + msg + "\n in eventQueue" );
							System.err.println(e);
						}
						finally
						{
							eventQueueMut.release();
						}
						
					}
					
					
				}
				catch(NumberFormatException ex)
				{
					System.err.println("failed to convert event number to integer for msg: " + msg);
					System.err.println(ex);
				}
				
				return;
			}
			
			eventQueue.add(temp); // Mutex 처리 부탁합니다.
		}
	}
	
	//새로운 서버에 연결하는 함수입니다. 받은 event의 EVENTNO가 0이면 미드웨어가 자동으로 호출을 합니다. 
	private void serverRedirect(String ipAddr, int portNumb)
	{
		setTempIpPort(ipAddr, portNumb);
		System.out.println("Started Server Redicect!");
		close();
		run();
		System.out.println("EventNo 0 is received and successfully processed!");
	}
	
	public CarEvent getEvent()
	{
		CarEvent temp = null;
		try
		{
			eventQueueMut.acquire();
			temp = eventQueue.remove();
		}
		catch(InterruptedException e)
		{
			System.err.println("error in getting msg" );
			System.err.println(e);
		}
		finally
		{
			eventQueueMut.release();
		}
		
		return temp;
	}
	
	private String converToString(CarEvent eve)
	{
		return eve.toString() + "\r\n";
	}
	
	// 새로운 accessPoint를 ip주소와 포트넘버를 사용하여 지정합니다.이 함수는 ipPort.txt의 내용을 바꿉니다. ipPort.txt에 직접 바꿔도 상관없습니다. 
	public boolean setAccessPoint(String ip, int portNumb)
	{
		Path path = Paths.get(IPPORT);
		
		if(!checkIpFormat(ip))
		{
			System.err.println("Upappropriate ip address passed! error in setAccessPoint!");
			return false;
		}
		
		try(BufferedWriter output = Files.newBufferedWriter(path, StandardOpenOption.WRITE))
		{
			output.write(IP_ADDRESS + ":" + ip +'\n');
			output.write( PORT_NUMB + ":" + portNumb + '\n');
			output.flush();
			
			return true;
			
		}
		catch(IOException ex)
		{
			System.err.println("Failed to write data in config.txt");
			return false;
		}
		

	}
	
	private void setTempIpPort(String ip, int portNumb)
	{
		
		this.ipAddr = ip;
		this.portNumb = portNumb;
	}
	
	private boolean checkIpFormat(String ipAdd)
	{
		System.out.println(ipAdd);
		String[] ipParts = splitWithChar(ipAdd, '.', 4);
		System.out.println(ipParts.length);
		if(ipParts.length != 4)
			return false;
		
		try
		{
			for(String part: ipParts)
			{
				Integer.parseInt(part);
			}
			
			return true;
			
		}
		catch(NumberFormatException e)
		{
			System.err.println("cannot convert IP address!: error in chekcIpFormat");
			System.err.println(e);
			return false;
		}
	}
	
	private String[] splitWithChar(String target , char a, int size)
	{
		String [] tempArr = new String[size];
		StringBuilder strBuilder = new StringBuilder();
		int retInd = 0;
		for(int ind = 0 ; ind < target.length(); ind++ )
		{
			if(target.charAt(ind) == a)
			{
				tempArr[retInd] = strBuilder.toString();
				retInd++; 
				if(retInd == size)
					break;
				strBuilder.setLength(0);
			}
			else
			{
				strBuilder.append(target.charAt(ind));
			}
		} 
		
		if(retInd < size)
		{
			tempArr[retInd] = strBuilder.toString();
			retInd++;
		}
		
		String [] retArr = new String[retInd];
		for(int i = 0; i < retInd; i++)
		{
			retArr[i] = tempArr[i];
		}
		
		return retArr;
	}
	

}