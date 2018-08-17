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
    private DatagramSocket dsocket = null;
    private ServerSocket server = null;
    private DataInputStream in = null;
    private DataOutputStream out = null;
    private FileInputStream fis = null;
    private FileOutputStream fos = null;
 
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

                        ClientHandler cl = new ClientHandler(socket, dsocket , Name, in, out, RoomList, fis, fos);
                        //Thread th = new Thread(cl);
                        cl.RoomLobby();

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
    Vector <ClientHandler> ClientList;
    
    public Room(String name)
    {
        this.name = name;
        this.ClientList = new Vector<>();
    }
    
    public String GetName()
    {
        return(this.name);
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
    ClientHandler thisCL = this;
    private String name;
    final DataInputStream dis;
    final DataOutputStream dos;
    final FileInputStream fis;
    final FileOutputStream fos;
    private String room;
    Vector <Room> RoomList;
    Socket s;
    DatagramSocket ds;
    boolean isloggedin;
     
    public ClientHandler(Socket s, DatagramSocket ds, String name,
                            DataInputStream dis, DataOutputStream dos, Vector <Room> RoomList, 
                            FileInputStream fis, FileOutputStream fos) {
        this.dis = dis;
        this.dos = dos;
        this.name = name;
        this.s = s;
        this.isloggedin = true;
        this.room = "lobby";
        this.RoomList = RoomList;
        this.fis = fis;
        this.fos = fos;
        this.ds = ds;
    }
 
    public String GetName()
    {
        return this.name;
    }
    
    public void Exec() 
    {
        String receivedT;
        String rname = thisCL.room;

        Room thisRoom = RoomList.firstElement();
        Iterator <Room> Iter = RoomList.iterator();
        while(Iter.hasNext())
        {
            Room cRoom = Iter.next();
            if(rname.equals(cRoom.GetName()))
            {
                thisRoom = cRoom;
                break;
            }
        }

        while (true) 
        {
            try
            {
                receivedT = dis.readUTF();
                StringTokenizer st = new StringTokenizer(receivedT);
                String tok = st.nextToken();
                
                System.out.println(name+": "+receivedT);

                if(tok.equals("logout"))
                {
                    for (ClientHandler cl : thisRoom.ClientList)
                    {
                        if(cl.isloggedin)
                            cl.dos.writeUTF(name+" has logged out");
                    }
                    isloggedin = false;
                    s.close();
                    break;
                }
                else if(tok.equalsIgnoreCase("exit"))
                {
                    thisCL.room = "lobby";
                    thisRoom.RemoveClient(thisCL);
                    for (ClientHandler cl : thisRoom.ClientList)
                    {
                        cl.dos.writeUTF(name+" has left the room");
                    }
                    RoomLobby();
                    return;                    
                }
                else if(tok.equalsIgnoreCase("add"))
                {
                    if(st.countTokens() < 1)
                    {
                        dos.writeUTF("Invalid Command, Please Retry");
                        continue;
                    }
                    String name = st.nextToken();
                    for(ClientHandler cl: Server.ClientList)
                    {
                        if(name.equals(cl.GetName()))
                        {
                            if(cl.room.equals("lobby"))
                            {
                                cl.room = thisCL.room;
                                thisRoom.AddClient(cl);
                                for (ClientHandler Rcl : thisRoom.ClientList)
                                {
                                    Rcl.dos.writeUTF(name+" has been added to the room");
                                }
                                continue;                               
                            }
                            else
                            {
                                dos.writeUTF("The Client associated with the name is already part of another Chat room");
                                continue;
                            }
                        }
                                
                    }
                    dos.writeUTF("Client with associated name doesnt exist.");
                    continue;
                   
                    
                }
                else if(tok.equalsIgnoreCase("list"))
                {
                    dos.writeUTF("------------------------------------------------------------");
                    for(ClientHandler cl : thisRoom.ClientList)
                    {
                        dos.writeUTF(cl.GetName());
                    }
                    dos.writeUTF("------------------------------------------------------------");
                    continue;                    
                }
                for (ClientHandler cl : thisRoom.ClientList)
                {
                    cl.dos.writeUTF(name+" : "+receivedT);
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
            catch(Exception ex)
            {
                ex.printStackTrace();
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
    
    
    void RoomLobby()
    {
        class Lobby implements Runnable
        {            
            ClientHandler CL;
            public Lobby(ClientHandler cl)
            {
                CL = cl;
            }
            public void DisplayInstructions(DataOutputStream dos)
            {
                try
                {
                    dos.writeUTF("Select one of the following commands:");
                    dos.writeUTF("1) LIST - Displays list of currenlty active chat rooms");
                    dos.writeUTF("2) CREATE <Name of chat room> - Create a new chat room");
                    dos.writeUTF("3) ENTER <Name of chat room> - Enter an existing chat room");
                    dos.writeUTF("4) LOGOUT - To logout of the application");
                    dos.writeUTF("------------------------------------------------------------");
                }
                catch(IOException i)
                {
                    i.printStackTrace();
                }
                return;

            }
            public void run()
            {        
                while(room.equals("lobby"))
                {
                    try
                    {
                        DisplayInstructions(dos);
                        String received;
                        received = dis.readUTF();
                        StringTokenizer st = new StringTokenizer(received);
                        String tok = st.nextToken();
                        if(tok.equalsIgnoreCase("LIST"))
                        {
                           dos.writeUTF("------------------------------------------------------------");
                           for(Room room: RoomList)
                           {
                               String test = "lobby";
                               if(!test.equalsIgnoreCase(room.GetName()))
                               {
                                   dos.writeUTF(room.GetName());
                               }                      
                           }
                           dos.writeUTF("------------------------------------------------------------");
                        }
                        else if(tok.equalsIgnoreCase("CREATE"))
                        {
                            if(st.countTokens() < 1)
                            {
                                dos.writeUTF("Invalid Command, Please Try again");
                                continue;
                            }
                            String newrname = st.nextToken();
                            for(Room check: RoomList)
                            {
                                if(newrname.equalsIgnoreCase(check.GetName()))
                                {
                                    dos.writeUTF("Name Already Taken");
                                    continue;
                                }
                            }
                            Room newroom = new Room(newrname);
                            RoomList.add(newroom);
                        }
                        else if(tok.equalsIgnoreCase("ENTER"))
                        {
                            if(st.countTokens() < 1)
                            {
                                dos.writeUTF("Invalid Command, Please Try again");
                                continue;
                            }
                            String roomname = st.nextToken();
                            CL.room = roomname;
                            Room thisRoom = RoomList.firstElement();
                            Iterator <Room> Iter = RoomList.iterator();
                            while(Iter.hasNext())
                            {
                                Room cRoom = Iter.next();
                                if(roomname.equals(cRoom.GetName()))
                                {
                                    thisRoom = cRoom;
                                    break;
                                }
                            }
                            thisRoom.AddClient(CL);                           
                        }
                        else if(tok.equalsIgnoreCase("logout"))
                        {
                            for (ClientHandler cl : Server.ClientList)
                            {
                                if(cl.isloggedin)
                                    cl.dos.writeUTF(name+" has logged out");
                            }
                            isloggedin = false;
                            s.close();
                            return;                           
                        }
                        else
                        {
                            dos.writeUTF("Invalid command, Please Enter one of the following commands");
                        }
                    }
                    catch(IOException i)
                    {
                        i.printStackTrace();
                    }
                }
                Exec();
                return;     
            }
        }
        Thread t = new Thread(new Lobby(thisCL));
        t.start();
    }    
}