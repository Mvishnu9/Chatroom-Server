package Server;
//Hi!
import java.net.*;
import java.io.*;
import java.util.*;
 
public class Server
{    
    static Vector <ClientHandler> ClientList = new Vector<>();
    static Vector <Room> RoomList = new Vector<>();
    
    
    static int ClientCount = 0;
    
    private Socket socket = null;
    private ServerSocket server = null;
    private DataInputStream in = null;
    private DataOutputStream out = null;
 
    public Server(int port)
    {
        try
        {
            server = new ServerSocket(port);
            System.out.println("Server started");
            Room lobby = new Room("lobby");
            RoomList.add(lobby);
        }
        catch(IOException i)
        {
            System.out.println(i);
        }
        
        Thread AcceptComms = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    while(true)
                    {
                        socket = server.accept();
                        System.out.println("New client request received : " + socket);

                        in = new DataInputStream(socket.getInputStream());
                        out = new DataOutputStream(socket.getOutputStream());

                        String Name = "";
                        Boolean nameDist = true;
                        while(nameDist)
                        {
                            Name = in.readUTF();
                            nameDist = false;
                            for(ClientHandler c : Server.ClientList)
                            {
                                if(Name.equals(c.GetName()))
                                {
                                    out.writeUTF("Name Already Taken. Please enter a different name.");
                                    nameDist = true;
                                }
                            }
                            if(!nameDist)
                            {
                                break;
                            }    
                        }
                        System.out.println("Creating a new handler for this client...");

                        ClientHandler cl = new ClientHandler(socket, Name, in, out);
                        //Thread th = new Thread(cl);
                        cl.DecideRoom.start();

                        ClientList.add(cl);
                        //th.start();
                    }
                }
                catch(IOException i)
                {
                    i.printStackTrace();
                }
            }
        });
        
        Thread Cleaner = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    while(true)
                    {
                        Iterator<ClientHandler> iter = ClientList.iterator();
                        while (iter.hasNext()) 
                        {
                            ClientHandler cl = iter.next();

                            if (!cl.isloggedin)
                                iter.remove();
                        }                    
                    }
                }
                catch(ConcurrentModificationException c)
                {
                    System.out.println("ConcurrentModificationException - ignore");
                }
            }
        });
        
        AcceptComms.start();
        Cleaner.start();
    }
    public static void main(String args[])
    {
        Server server = new Server(5000);
    }
}

class Room
{
    private String name;
    static Vector <ClientHandler> ClientList;
    
    public Room(String name)
    {
        this.name = name;
    }
    
    public void AddClient(ClientHandler cl)
    {
        this.ClientList.add(cl);
    }
    
    public void RemoveClient(ClientHandler cl)
    {
        this.ClientList.remove(cl);
    }
    
}

class ClientHandler
{
    Scanner scn = new Scanner(System.in);
    private String name;
    final DataInputStream dis;
    final DataOutputStream dos;
    private String room;
    Socket s;
    boolean isloggedin;
     
    public ClientHandler(Socket s, String name,
                            DataInputStream dis, DataOutputStream dos) {
        this.dis = dis;
        this.dos = dos;
        this.name = name;
        this.s = s;
        this.isloggedin=true;
        this.room = "lobby";
        
    }
 
    public String GetName()
    {
        return this.name;
    }
    
    Thread DecideRoom = new Thread(new Runnable()
    {
        @Override
        public void run()
        {
            try
            {
                dos.writeUTF("Select one of the following commands:");
                dos.writeUTF("1) LIST - Displays list of currenlty active chat rooms");
                dos.writeUTF("2) CREATE - Create a new chat room and enter it");
                dos.writeUTF("3) ENTER <Name of chat room> - Enter an existing chat room");
                String received;
                received = dis.readUTF();
                StringTokenizer st = new StringTokenizer(received);
                String tok = st.nextToken();
                if(tok.equals("LIST"))
                {
                    
                    dos.writeUTF("Typed LIST");
                }
                else if(tok.equals("CREATE"))
                {
                    
                }
                else if(tok.equals("ENTER"))
                {
                    
                }
            }
            catch(IOException i)
            {
                i.printStackTrace();
            }
            Exec.start();
            return;     
        }
    });
    
    Thread Exec = new Thread(new Runnable() 
    {
        @Override
        public void run() 
        {
            String received;
            while (true) 
            {
                try
                {
                    received = dis.readUTF();

                    System.out.println(name+": "+received);

                    if(received.equals("logout"))
                    {
                        for (ClientHandler cl : Server.ClientList)
                        {
                            if(cl.isloggedin)
                                cl.dos.writeUTF(name+" has logged out");
                        }
                        isloggedin = false;
                        s.close();
                        break;
                    }
                    for (ClientHandler cl : Server.ClientList)
                    {
                        cl.dos.writeUTF(name+" : "+received);
                    }

                }
                catch(EOFException e)
                {
                    System.out.println("No clients present");
                    return;
                }
                catch(IOException i)
                {
                    i.printStackTrace();
                }             
            }

            try
            {
                dis.close();
                dos.close();
                return;             
            }
            catch(IOException i)
            {
                i.printStackTrace();                    
            }
        }
    });
}