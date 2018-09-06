package snake;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.Scanner;

public class PlayerInfo {
    int rank;
    private String name;
    int score;
    PlayerInfo(int rank, String name, int Score){
        this.rank = rank;
        this.name = name;
        this.score = Score;
    }

    @Override
    public String toString() {
        return (rank + " " + name + "\t" + score);
    }
}

class PlayerInfoSet{
    private LinkedList<PlayerInfo> lstPlayers;
    private final URL urlRank = PlayerInfo.class.getResource("/resource/Rank.txt");
    PlayerInfoSet(){
        lstPlayers = new LinkedList<>();
        try{
            //System.out.print("PalyerInfo\n");
            //System.out.println(PlayerInfoSet.class.getResource(""));
            //System.out.println(urlRank.toURI().getPath());
            File fiRank = new File("src/resource/Rank.txt");
            System.out.println(fiRank.getAbsolutePath());
            Scanner console = new Scanner(fiRank, StandardCharsets.UTF_8);
            while(console.hasNextLine()){
                int rank = console.nextInt();
                System.out.println(rank);
                String name = console.next();
                int score = console.nextInt();
                lstPlayers.add(new PlayerInfo(rank, name, score));
            }
            console.close();
        }
        catch (FileNotFoundException e){
            System.out.println("No such file");
        }
        catch (IOException e){
            System.out.println("It's not encoded by UTF-8");
        }
    }

    int getNewRank(String name, int Score){
        int temp = lstPlayers.size() + 1;
        for(int i = 0; i < lstPlayers.size(); i++){
            if(Score > lstPlayers.get(i).score){
                if(temp > lstPlayers.get(i).rank) {
                    temp = lstPlayers.get(i).rank;
                }
                lstPlayers.get(i).rank++;
            }
        }
        lstPlayers.add(temp - 1, new PlayerInfo(temp, name, Score));
        if(lstPlayers.size() > 5){
            lstPlayers.removeLast();
        }
        return temp;
    }

    void saveRank(){
        try {
            File fiRank = new File("src/resource/Rank.txt");
            System.out.println(fiRank.getAbsolutePath());
            PrintStream out = new PrintStream(fiRank);
            for(int i = 0; i < lstPlayers.size(); i++){
                if(i != 0){
                    out.println();
                }
                out.print(lstPlayers.get(i).toString());
            }
            out.close();
        }
//        catch (URISyntaxException e){
//            System.out.println(e.getMessage());
//        }
        catch (FileNotFoundException e){
            System.out.println("No such file");
        }
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        for(int i = 0; i < lstPlayers.size(); i++){
            stringBuilder.append(lstPlayers.get(i) + ",");
        }
        stringBuilder.append("\n");
        return stringBuilder.toString();
    }
}
