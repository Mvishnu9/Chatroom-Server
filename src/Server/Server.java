
package Server;

import java.net.*;
import java.io.*;
import java.util.*;
 
public class Server
{    
    static Vector <ClientHandler> ClientList = new Vector<>();
    static Vector <Room> RoomList = new Vector<>();
        
    static int ClientCount = 0;
    
    private Socket socket = null;
    private Socket Fsocket = null;
    private DatagramSocket dsocketR = null;
    private DatagramSocket dsocketS = null;
    private ServerSocket server = null;
    private ServerSocket Fserver = null;
    private DataInputStream in = null;
    private DataOutputStream out = null;
    private DataInputStream Fin = null;
    private DataOutputStream Fout = null;

 
    public Server(int port)
    {
        try
        {

            server = new ServerSocket(port);
            Fserver = new ServerSocket(5004);
            dsocketR = new DatagramSocket(port+1);
            dsocketS = new DatagramSocket();
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
                        Fsocket = Fserver.accept();
                        System.out.println("New client request received : " + socket);

                        in = new DataInputStream(socket.getInputStream());
                        out = new DataOutputStream(socket.getOutputStream());
                        Fin = new DataInputStream(Fsocket.getInputStream());
                        Fout = new DataOutputStream(Fsocket.getOutputStream());
                        

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

                        ClientHandler cl = new ClientHandler(socket, dsocketR, dsocketS, Name, in, out, RoomList, Fin, Fout);
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
    private int count;
    
    public Room(String name)
    {
        this.name = name;
        this.ClientList = new Vector<>();
        this.count = 0;
    }
    
    public String GetName()
    {
        return(this.name);
    }
    
    public int GetCount()
    {
        return(this.count);
    }
    
    public void AddClient(ClientHandler cl)
    {
        this.ClientList.add(cl);
        count++;
    }
    
    public void RemoveClient(ClientHandler cl)
    {
        this.ClientList.remove(cl);
        count--;
    }
    
}

class ClientHandler
{
    Scanner scn = new Scanner(System.in);
    ClientHandler thisCL = this;
    private String name;
    final DataInputStream dis;
    final DataOutputStream dos;
    final DataInputStream fis;
    final DataOutputStream fos;
    private String room;
    Vector <Room> RoomList;
    Socket s;
    DatagramSocket dsR;
    DatagramSocket dsS;
    private byte[] receive;
    boolean isloggedin;
     
    public ClientHandler(Socket s, DatagramSocket ds, DatagramSocket dss, String name,
                            DataInputStream dis, DataOutputStream dos, Vector <Room> RoomList, 
                            DataInputStream fis, DataOutputStream fos) {
        this.dis = dis;
        this.dos = dos;
        this.name = name;
        this.s = s;
        this.isloggedin = true;
        this.room = "lobby";
        this.RoomList = RoomList;
        this.fis = fis;
        this.fos = fos;
        this.dsR = ds;
        this.dsS = dss;
    }
 
    public String GetName()
    {
        return this.name;
    }
    
    public void DestroyRoom(Room room)
    {
        Iterator<Room> iter = RoomList.iterator();
        while (iter.hasNext()) 
        {
            Room r = iter.next();
            if(r.equals(room))
                iter.remove();
        } 
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
                StringTokenizer st2 = new StringTokenizer(receivedT, ":");
                String tok2 = "";
                int ct = st2.countTokens();
                if(ct == 3)
                {
                    tok2 = st2.nextToken();
                }

                String tok = st.nextToken();
                
                System.out.println(name+" : "+receivedT);

                if(tok.equals("logout"))
                {
                    for (ClientHandler cl : thisRoom.ClientList)
                    {
                        if(cl.isloggedin)
                            cl.dos.writeUTF(name+" has logged out");
                    }
                    isloggedin = false;
                    thisRoom.RemoveClient(thisCL);
                    dos.writeUTF("Successfully logged out");
                    s.close();
                    dis.close();
                    dos.close();
                    return;
                }
                else if(tok.equalsIgnoreCase("exit"))
                {
                    thisCL.room = "lobby";
                    thisRoom.RemoveClient(thisCL);
                    if(thisRoom.GetCount() == 0)
                    {
                        for (ClientHandler cl : Server.ClientList)
                        {
                            if(cl.room.equalsIgnoreCase("lobby"))
                            {
                                cl.dos.writeUTF("-----------------------------------------------------------");
                                cl.dos.writeUTF("The chatroom "+thisRoom.GetName()+" has been deleted due to lack of participants");
                                cl.dos.writeUTF("-----------------------------------------------------------");
                            }
                        }
                        DestroyRoom(thisRoom);
                    }
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
                else if((ct == 3) && (tok2.equalsIgnoreCase("Sending file UDP ")))
                {
                    String Len = st2.nextToken();
                    Len = st2.nextToken();
                    Long len = Long.parseLong(Len);
                    System.out.println("Length "+Len);
                    receive = new byte[4096];
                    for (ClientHandler cl : thisRoom.ClientList)
                    {
                        cl.dos.writeUTF(name+" : "+receivedT);
                    }
                    DatagramPacket DpReceive = new DatagramPacket(receive, receive.length);
                    dsR.receive(DpReceive);
                    receive = DpReceive.getData();
                    String P = new String(receive, 0, DpReceive.getLength());
                    System.out.println(name+" : " + P);
                    for (ClientHandler cl : thisRoom.ClientList)
                    {
                        InetAddress ina = cl.s.getInetAddress();
                        DatagramPacket DpSend = new DatagramPacket(receive, receive.length, ina, 5003);
                        dsS.send(DpSend);
//                        cl.dos.writeUTF(name+" : "+P);
                    }
                    continue;
                }
                else if((ct == 3) && (tok2.equalsIgnoreCase("Sending file TCP ")))
                {
                    String Len = st2.nextToken();
                    Len = st2.nextToken();
                    Long len = Long.parseLong(Len);
                    System.out.println("Length of file = "+Len);
                    receive = new byte[4096];
                    for (ClientHandler cl : thisRoom.ClientList)
                    {
                        cl.dos.writeUTF(name+" : "+receivedT);
                    }
                    int count;
                    while((len>0)&&((count = fis.read(receive))>0))
                    {
                        len = len - 4096;
                        for (ClientHandler cl : thisRoom.ClientList)
                        {
                            cl.fos.write(receive);
                        }
                        receive = new byte[4096];
                    }
                    for (ClientHandler cl : thisRoom.ClientList)
                    {
                        cl.fos.flush();
                    }
                    continue;
                }
                for (ClientHandler cl : thisRoom.ClientList)
                {
                    cl.dos.writeUTF(name+" : "+receivedT);
                }

            }
            catch(SocketException s)
            {
                s.printStackTrace();
                return;
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
            private void Selection()
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
                            Boolean tflag = true;
                            for(Room check: RoomList)
                            {
                                if(newrname.equalsIgnoreCase(check.GetName()))
                                {
                                    dos.writeUTF("Name Already Taken");
                                    tflag = false;
                                }
                            }
                            if(!tflag)
                            {
                                continue;
                            }
                            Room newroom = new Room(newrname);
                            RoomList.add(newroom);
                            CL.room = newrname;
                            newroom.AddClient(CL);                            
                        }
                        else if(tok.equalsIgnoreCase("ENTER"))
                        {
                            if(st.countTokens() < 1)
                            {
                                dos.writeUTF("Invalid Command, Please Try again");
                                continue;
                            }
                            String roomname = st.nextToken();
                            Room thisRoom = RoomList.firstElement();
                            Iterator <Room> Iter = RoomList.iterator();
                            Boolean tflag = false;
                            while(Iter.hasNext())
                            {
                                Room cRoom = Iter.next();
                                if(roomname.equals(cRoom.GetName()))
                                {
                                    thisRoom = cRoom;
                                    tflag = true;
                                    break;
                                }
                            }
                            if(!tflag)
                            {
                                dos.writeUTF("Chatroom doesnt exist, please create using CREATE command");
                                continue;
                            }
                            CL.room = roomname;
                            thisRoom.AddClient(CL);                           
                        }
                        else if(tok.equalsIgnoreCase("logout"))
                        {
                            for (ClientHandler cl : Server.ClientList)
                            {
                                if(cl.isloggedin)
                                    cl.dos.writeUTF(name+" has logged out");
                            }
                            dos.writeUTF("Successfully logged out");
                            s.close();
                            dis.close();
                            dos.close();
                            fis.close();
                            fos.close();
                            isloggedin = false;
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
            }
            public void run()
            {        
                Selection();
                if(thisCL.isloggedin)
                {
                    Exec();
                }
                return;     
            }
        }
        Thread t = new Thread(new Lobby(thisCL));
        t.start();
    }    
}