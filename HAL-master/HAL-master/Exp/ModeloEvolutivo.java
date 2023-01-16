package Exp;

import HAL.GridsAndAgents.AgentGrid2D;
import HAL.GridsAndAgents.AgentSQ2D;
import HAL.GridsAndAgents.AgentSQ2Dunstackable;
import HAL.Gui.GridWindow;
import HAL.Rand;
import HAL.Util;
import org.lwjgl.opencl.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static Exp.DataStorage.*;
import static Exp.ModeloEvolutivo.StoreLine;

import static org.lwjgl.opencl.CL10.*;
import static org.lwjgl.opencl.CL10.clReleaseContext;

import java.lang.System;



public class ModeloEvolutivo extends AgentGrid2D<Celula> {
    String filepath;
    long seed;
    Rand rng;
    int maxima;
    int maximumFood;
    ArrayList<Neural> networks = new ArrayList<>();

    ArrayList<String> Dictionary = new ArrayList<>();
    ArrayList<DataStorage> Data=new ArrayList<>();
    ArraysManagement AC = new ArraysManagement();
    int maxindex;
    float muta, strength;
    int starve, apop, move, reproduce, active;
    int[] divHood = Util.VonNeumannHood(false);
    int[][] food;
    int[][] vecinity;

    public int FindIndex(int index) {
        for (int i = 0; i < networks.size(); i++) {
            if (networks.get(i).index == index) {
                return i;
            }
        }
        System.out.println(index);
        return -1;
    }

    public ModeloEvolutivo(int x, int y, int initial, int maxima,int iter,long seed) {
        super(x, y, Celula.class, true, true);
        this.seed=seed;
        rng=new Rand(seed);
        starve = 0;
        apop = 0;
        move = 0;
        active = 0;
        muta = 0;
        strength = 0;
        reproduce = 0;
        filepath = "C:\\Users\\bast_\\OneDrive\\Escritorio\\HalSim\\OutPutFile\\"+iter+"/";

        food = new int[xDim][yDim];
        vecinity= new int[xDim][yDim];
        AC.ConstantThis(food, initial);
        this.maxima = maxima;
    }
    static public void StoreLine(String filename, String lineToAppend) {
        if (!new File(filename).exists()) {
            File file = new File(filename);
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            FileWriter myWriter = new FileWriter(filename, true);
            myWriter.write(lineToAppend + "\n");
            myWriter.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public void Load(String filename) {
        try {
            File myObj = new File(filepath + filename);
            Scanner myReader = new Scanner(myObj);
            int i = 0, x, y;
            networks.clear();
            while (myReader.hasNextLine()) {
                x = i / yDim;
                y = i % yDim;
                String data = myReader.nextLine();
                String[] subdata = data.split(";");
                food[x][y] = Integer.parseInt(subdata[0]);
                String[] subdata2 = subdata[1].split(",");
                if (subdata.length > 3) {
                    String[] subdata3 = subdata[2].split(",");
                    int[] ports = new int[subdata3.length];
                    for (int j = 0; j < subdata3.length; j++) {
                        ports[j] = Integer.parseInt(subdata3[j]);
                    }
                    int index = Integer.parseInt(subdata[3]);
                    networks.add(new Neural(ports, index));
                    Data.add(new DataStorage("Especie"+index));
                    networks.get(i).population = Integer.parseInt(subdata[4]);
                    for (int j = 0; j < subdata.length - 5; j++) {
                        String weights = subdata[5 + j].split(":")[0];
                        String[] weight = weights.split("'");
                        for (int k = 0; k < weight.length; k++) {
                            String[] rows = weight[k].split(",");
                            for (int l = 0; l < rows.length; l++) {
                                networks.get(i).pesos.get(j)[k][l] = Float.parseFloat(rows[l]);
                            }
                        }
                        String biases = subdata[5 + j].split(":")[1];
                        String[] bias = biases.split(",");
                        for (int k = 0; k < bias.length; k++) {
                            networks.get(i).umbrales.get(j)[k] = Float.parseFloat(bias[k]);
                        }
                    }
                }

                if (GetAgent(i) != null) {
                    GetAgent(i).Dispose();
                }
                if (subdata2.length > 1) {
                    NewAgentSQ(i).Init(Integer.parseInt(subdata2[0]), Float.parseFloat(subdata2[3]), Float.parseFloat(subdata2[2]));

                    GetAgent(i).energy = Integer.parseInt(subdata2[1]);
                }
                i++;
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }

    public void Save(String filename, int sufix) {
        File tempFile = new File(filepath + filename + sufix + ".txt");
        boolean exists = tempFile.exists();
        if (!exists) {

            try {
                tempFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Writer output;
        int x, y;
        try {
            output = new BufferedWriter(new FileWriter(filepath + filename + sufix + ".txt", true));
            for (int i = 0; i < xDim * yDim; i++) {
                x = i / yDim;
                y = i % yDim;
                String cell;
                StringBuilder neuron = new StringBuilder();
                if (GetAgent(i) != null) {
                    cell = GetAgent(i).index + "," + GetAgent(i).energy + "," + GetAgent(i).strength + "," + GetAgent(i).mutability;
                } else {
                    cell = "0";
                }
                if (i < networks.size()) {
                    for (int j = 0; j < networks.get(i).ports.length; j++) {
                        if (j != 0) {
                            neuron.append(",");
                        }
                        neuron.append(networks.get(i).ports[j]);
                    }
                    neuron.append(";").append(networks.get(i).index).append(";").append(networks.get(i).population).append(";");
                    for (int layer = 0; layer < networks.get(i).nlayers; layer++) {
                        for (int row = 0; row < networks.get(i).umbrales.get(layer).length; row++) {
                            for (int col = 0; col < networks.get(i).pesos.get(layer)[0].length; col++) {
                                neuron.append(networks.get(i).pesos.get(layer)[row][col]);
                                if (col < networks.get(i).pesos.get(layer)[0].length - 1) {
                                    neuron.append(",");
                                } else if (row < networks.get(i).umbrales.get(layer).length - 1) {
                                    neuron.append("'");
                                } else {
                                    neuron.append(":");
                                }
                            }

                        }
                        for (int row = 0; row < networks.get(i).umbrales.get(layer).length; row++) {
                            neuron.append(networks.get(i).umbrales.get(layer)[row]);
                            if (row < networks.get(i).umbrales.get(layer).length - 1) {
                                neuron.append(",");
                            }
                        }
                        neuron.append(";");
                    }
                } else {
                    neuron = new StringBuilder("0");
                }
                output.append(String.valueOf(food[x][y])).append(";").append(cell).append(";").append(neuron.toString()).append("\n");
            }
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void Setup(int maxindex,int initFood,float Mstrength,int[] arquitecture) {
        for (int i = 1; i <= maxindex; i++) {
            NewAgentSQ( rng.Int(xDim), rng.Int(yDim)).Init(i, 0.04f, Mstrength,initFood);
            networks.add(new Neural(arquitecture,i));
            networks.get(i-1).Randomize(rng.Long(2000));
            Data.add(new DataStorage("Especie"+i));
            this.maxindex=maxindex;
        }
    }

    public void Feed(int frequency, int density) {
        for (int i = 0; i < frequency; i++) {
            int x = rng.Int(xDim);
            int y = rng.Int(yDim);
            food[x][y] = Math.min((food[x][y] + density), maximumFood);
        }
    }

    public void Feed2(int cantidad,int frequency){
        for (int x = 0; x < xDim; x++) {
            for (int y = 0; y < yDim; y++) {
                int up=Math.floorMod(y+1, yDim);
                int down=Math.floorMod(y-1 ,yDim);
                int left= Math.floorMod(x-1 ,xDim);
                int right= Math.floorMod(x+1 ,xDim);
                int sum=food[x][up]+food[x][down]+food[left][y]+food[right][y]+food[x][y];
                if(rng.Double()<((float)frequency)/(xDim*yDim)*Math.exp(-Math.pow(((float)sum/(maximumFood)-2.5f),2))) {
                    food[x][y] = Math.min((food[x][y] + cantidad), maximumFood);
                }
            }
        }
    }
    public void Feed3(int cantidad,int frequency){
        for (int x = 0; x < xDim; x++) {
            for (int y = 0; y < yDim; y++) {
                int up=Math.floorMod(y+1, yDim);
                int[][] food2=food;
                int down=Math.floorMod(y-1 ,yDim);
                int left= Math.floorMod(x-1 ,xDim);
                int right= Math.floorMod(x+1 ,xDim);
                int sum=food2[x][up]+food2[x][down]+food2[left][y]+food2[right][y]+food2[x][y];
                if(rng.Double()<((float)frequency)/(xDim*yDim)*Math.exp(-Math.pow(((float)sum/(maximumFood)-2.5f),2))) {
                    food[x][y] = Math.min((food[x][y] + cantidad), maximumFood);
                }
            }
        }
    }
    public void Draw(GridWindow win,int max) {
        int x,y;
        double c;
        for (int i = 0; i < xDim * yDim; i++) {
            x = i / yDim;
            y = i % yDim;
            c = (float) food[x][y]/max ;

            if (GetAgent(i) != null) {
                float colorcito=((float) FindIndex(GetAgent(i).index)) / networks.size();
                win.SetPix(i, Util.RGB(colorcito, colorcito, 1));
            } else {
                win.SetPix(i, Util.RGB(c, c, 0));
                //win.SetPix(i, Util.RGB(0, 0, 0));
            }
        }
    }

    public void Zero() {
        starve = 0;
        apop = 0;
        reproduce = 0;
        move = 0;
        active = 0;
        strength = 0;
        muta = 0;
    }

    public void CheckPop(){
        for (int i = 0; i < xDim; i++) {
            for (int j = 0; j < yDim; j++) {
                vecinity[i][j]=this.PopAt(i,j);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Rand rng=new Rand(5984L);
        int iter = 78;
        while (iter > 77) {
            float MStrength;
            if(iter<26){
                 MStrength=0.10f;
            }else if(iter<51){
                MStrength=1;
            }else if(iter<76){
                MStrength=10;
            }else{
                MStrength=100;
            }

            int saveTime = 500;
            int time = 0;
            int xDim=200 , yDim=200;

            int initialNumber=500;
            int BeginerBoost=5;
            int[] arquitecture;


            int density = 1;
            int scale = 600 / Math.max(xDim, yDim);
            int frequency = (int) ((.1f * xDim * yDim) / density);
            GridWindow win = new GridWindow( xDim, yDim, scale);
            int initial = 10;
            int maxima = 10;

            long seed= rng.Long(10000);

            ModeloEvolutivo modelo = new ModeloEvolutivo(xDim, yDim, initial, maxima,iter,seed);
            File directory = new File(modelo.filepath);
            if (! directory.exists()){
                directory.mkdir();
            }
            StoreLine(modelo.filepath+"MetaData.txt",1+(iter-1)/20+";"+seed);
            StoreLine(modelo.filepath+"Description.txt","Semilla:"+modelo.seed+", xDim:"+xDim+", yDim:"+yDim +", initial:"+initial+", maxima:"+maxima+", density:"+density+", frequency:"+frequency );
            modelo.maximumFood = 20;
            modelo.Dictionary.add("Random");
            modelo.Dictionary.add("Energy");
            modelo.Dictionary.add("UnderFood");
            modelo.Dictionary.add("NorthFood");
            modelo.Dictionary.add("SouthFood");
            modelo.Dictionary.add("WestFood");
            modelo.Dictionary.add("EastFood");
            modelo.Dictionary.add("UnderNeigh");
            modelo.Dictionary.add("NorthNeigh");
            modelo.Dictionary.add("SouthNeigh");
            modelo.Dictionary.add("WestNeigh");
            modelo.Dictionary.add("EastNeigh");

            arquitecture=new int[]{modelo.Dictionary.size(),25,7};

            modelo.Setup(initialNumber,BeginerBoost,MStrength,arquitecture);
            modelo.CheckPop();
            //modelo.Load("Saved21.txt");
            long starting = System.currentTimeMillis();
            long minute;
            long hour;
            long second;
            modelo.active = 1;

            while (modelo.networks.size() > 0 && (time < 10000)) {
                modelo.Data.add(new DataStorage("Tiempo" + time));
                second = ((System.currentTimeMillis() - starting) / 1000) % 60;
                minute = ((System.currentTimeMillis() - starting) / 60000) % 60;
                hour = ((System.currentTimeMillis() - starting) / 3600000);
                if (time % saveTime == 0 && time > 0) {
                    modelo.Save("Saved", time / saveTime);
                }

                if (!new File(modelo.filepath + "TestData.txt").exists()) {
                    File file = new File(modelo.filepath + "TestData.txt");
                    try {
                        file.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                try {
                    for (int i = 0; i < modelo.networks.size(); i++) {
                        FileWriter myWriter = new FileWriter(modelo.filepath + "TestData.txt", true);
                        String TimeData = time + ";" + modelo.networks.get(i).index + ";" + modelo.networks.get(i).population + ";";
                        myWriter.write(TimeData + "\n");
                        myWriter.close();
                    }
                } catch (IOException e) {
                    System.out.println("An error occurred.");
                    e.printStackTrace();
                }

                String Activity = modelo.starve + ";" + modelo.reproduce + ";" + modelo.move + ";" + modelo.apop + ";" + modelo.active;
                StoreLine(modelo.filepath + "Activity.txt", Activity);
                modelo.Zero();

                modelo.ShuffleAgents(modelo.rng);
                float AverageStrength =0;
                float AverageFrequency =0;
                int countOfCells = 0;
                for (Celula cell : modelo) {
                    cell.Step(time);
                    countOfCells ++;
                    AverageFrequency += cell.mutability;
                    AverageStrength +=cell.strength;

                }
                StoreLine(modelo.filepath + "Mutabilidad.txt", time+";"+AverageStrength/countOfCells+";"+AverageFrequency/countOfCells );

                //modelo.Feed(frequency, density);
                modelo.Feed3(density,frequency);
                modelo.Draw(win, modelo.maximumFood);
                win.TickPause(0);
                System.out.println(hour + ":" + minute + ":" + second + "  ;" + time+ "  ;" + modelo.Pop());
                //Delete(modelo.Data, "Tiempo" + time, modelo.Dictionary, modelo.filepath);
                Delete2(modelo.Data, "Tiempo" + time);
                ++time;
            }

            modelo.Save("Saved", (1 + time / saveTime));
            while (modelo.Data.size() > 0) {
                DataStorage data = modelo.Data.get(0);
                String info = Collect(modelo.Data, data.ID, modelo.Dictionary);
                int index = Integer.parseInt(data.ID.replaceAll("\\D+", ""));
                StoreLine(modelo.filepath + "GenData.txt", modelo.networks.get(modelo.FindIndex(index)).index + ";" + modelo.networks.get(modelo.FindIndex(index)).parentIndex + ";" + modelo.networks.get(modelo.FindIndex(index)).census + ";" + modelo.networks.get(modelo.FindIndex(index)).generation +";"+modelo.networks.get(modelo.FindIndex(index)).bornedAt+";"+time+ info);
                //Delete(modelo.Data, data.ID, modelo.Dictionary, modelo.filepath);
                Delete2(modelo.Data, data.ID);
            }
            iter--;
        }
        System.exit(100);
    }
}


class Celula extends AgentSQ2D<ModeloEvolutivo> {
    int index;
    int energy;
    float mutability, strength;
    int order;
    public void Init(int index, float mutability, float strength) {
        energy = 1;
        order=-1;
        this.index = index;
        this.strength = strength;
        this.mutability = mutability;
    }
    public void Init(int index, float mutability, float strength,int food) {
        energy = food;
        order=-1;
        this.index = index;
        this.strength = strength;
        this.mutability = mutability;
    }

    public void Step(int time) {

        assert G != null;
        int bound = 10;
        float rng = (float) G.rng.Int(bound);
        G.muta += this.mutability;
        G.strength += this.strength;
        G.active++;
        int up=Math.floorMod(G.ItoY(this.Isq())-1,G.yDim);
        int down=Math.floorMod((G.ItoY(this.Isq())+1),G.yDim);
        int thisx=G.ItoX(this.Isq());
        int thisy=G.ItoY(this.Isq());
        int left=(Math.floorMod((G.ItoX(this.Isq())-1),G.xDim));
        int right=(Math.floorMod((G.ItoX(this.Isq())+1),G.xDim));
        int upneigh= G.vecinity[thisx][up];
        int thidneigh= G.vecinity[thisx][thisy];
        int downneigh= G.vecinity[thisx][down];
        int leftneigh= G.vecinity[left][thisy];
        int rightneigh= G.vecinity[right][thisy];
        int upfood=G.food[thisx][up];
        int leftfood=G.food[left][thisy];
        int rightfood=G.food[right][thisy];
        int downfood=G.food[thisx][down];
        int thisfood=G.food[thisx][thisy];
        float[] input = new float[]{rng, this.energy,thisfood,upfood,downfood,rightfood,leftfood,thidneigh,upneigh,downneigh,rightneigh,leftneigh,1};



        float[] output = G.networks.get(G.FindIndex(index)).Compute(input, 1);
        float[] dz=new float[input.length];
        dz[1] = 0;//G.networks.get(G.FindIndex(index)).TestDiameter(input, 0, G.maxima, 1);
        dz[0] = 0;//G.networks.get(G.FindIndex(index)).TestDiameter(input, 0, bound-1, 0);
        dz[2] = 0;//G.networks.get(G.FindIndex(index)).TestDiameter(input, 0, 4, 2);
        dz[3] = 0;//G.networks.get(G.FindIndex(index)).TestDiameter(input, 0, G.maximumFood, 3);
        int responce = G.AC.getIndexOfLargest(output);
        if (output[responce] < 0) {
            responce = 3;
        }
        int action = responce;
        this.energy--;
        if (this.energy < 0) {
            G.starve++;

            action = 1;
        } else {
            switch (responce) {
                case 0 -> {
                    action = 3;
                    if (this.energy >= 2) {
                        if (Reproduce(time)) {
                            this.energy -= 2;
                            G.reproduce++;
                            action = 0;
                        }
                    }
                }
                case 1 -> G.apop++;
                case 2 -> {
                    action = 3;
                    if (this.energy >= 1) {
                        if (MoveRandom()) {
                            this.energy--;
                            G.move++;
                            action = 2;
                        }
                    }
                }
                case 3 -> {
                    action = 3;
                    if (this.energy >= 1) {
                        if (MoveUp()) {
                            this.energy--;
                            G.move++;
                            action = 2;
                        }
                    }
                }
                case 4 -> {
                    action = 3;
                    if (this.energy >= 1) {
                        if (MoveDown()) {
                            this.energy--;
                            G.move++;
                            action = 2;
                        }
                    }
                }
                case 5 -> {
                    action = 3;
                    if (this.energy >= 1) {
                        if (MoveRight()) {
                            this.energy--;
                            G.move++;
                            action = 2;
                        }
                    }
                }
                case 6 -> {
                    action = 3;
                    if (this.energy >= 1) {
                        if (MoveLeft()) {
                            this.energy--;
                            G.move++;
                            action = 2;
                        }
                    }
                }
            }
        }


        FindDS(G.Data, "Especie" + index).AddData(G.Dictionary, input, action, responce, dz);
        FindDS(G.Data, "Tiempo" + time).AddData(G.Dictionary, input, action, responce, dz);
        if (this.energy < 0) {
            this.energy = 0;
        }
        if (action == 1) {
            Die(time);
        }
        if (this.energy < G.maxima) {
            int remanent = G.maxima - energy;
            if (remanent <= G.food[G.ItoX(Isq())][G.ItoY(Isq())]) {
                G.food[G.ItoX(Isq())][G.ItoY(Isq())] -= remanent;
                this.energy += remanent;
            } else {
                this.energy += G.food[G.ItoX(Isq())][G.ItoY(Isq())];
                G.food[G.ItoX(Isq())][G.ItoY(Isq())] = 0;
            }
        }

    }
    public void Die(int time) {
        assert G != null;
        G.food[G.ItoX(Isq())][G.ItoY(Isq())] += Math.max(0, energy);
        G.vecinity[G.ItoX(Isq())][G.ItoY(Isq())]--;

        Dispose();
        G.networks.get(G.FindIndex(index)).population--;
        if (G.networks.get(G.FindIndex(index)).population == 0) {
            String data=Collect(G.Data,"Especie"+index, G.Dictionary);
            StoreLine(G.filepath+"GenData.txt",G.networks.get(G.FindIndex(index)).index+";"+G.networks.get(G.FindIndex(index)).parentIndex+";"+G.networks.get(G.FindIndex(index)).census+";"+G.networks.get(G.FindIndex(index)).generation+";"+G.networks.get(G.FindIndex(index)).bornedAt+";"+time+data);
            Delete(G.Data,"Especie"+index, G.Dictionary,G.filepath);
            G.networks.remove(G.FindIndex(index));
        }
    }

    public boolean MoveRandom() {
        assert G != null;
        //int option = MapEmptyHood(G.divHood);
        int option =G.rng.Int(4);
        //if (option > 0) {

            //MoveSafeSQ(G.ItoX(place), G.ItoY(place));
        switch (option) {
            case 0 -> {
                return MoveUp();
            }
            case 1 -> {
                return MoveDown();
            }
            case 2 -> {
                return MoveLeft();
            }
            case 3 -> {
                return MoveRight();
            }
            default -> {
                return false;
            }
        }

        //} else {
          //  return false;
        //}
    }
    public boolean MoveUp() {
        assert G != null;
        //int option = MapEmptyHood(G.divHood);
        //int option = MapHood(G.divHood);
        //if (option > 0) {
            //MoveSafeSQ(G.ItoX(place), G.ItoY(place));
            G.vecinity[G.ItoX(this.Isq())][Math.floorMod(G.ItoY(this.Isq())-1,G.yDim)]++;
            G.vecinity[G.ItoX(this.Isq())][G.ItoY(this.Isq())]--;
            MoveSQ(G.ItoX(this.Isq()), Math.floorMod(G.ItoY(this.Isq())-1,G.yDim));

            return true;
       // } else {
         //   return false;
        //}
    }
    public boolean MoveDown() {
        assert G != null;
        //int option = MapEmptyHood(G.divHood);
        //int option = MapHood(G.divHood);
        //if (option > 0) {
        //MoveSafeSQ(G.ItoX(place), G.ItoY(place));

        G.vecinity[G.ItoX(this.Isq())][Math.floorMod(G.ItoY(this.Isq())+1,G.yDim)]++;
        G.vecinity[G.ItoX(this.Isq())][G.ItoY(this.Isq())]--;
        MoveSQ(G.ItoX(this.Isq()), Math.floorMod(G.ItoY(this.Isq())+1,G.yDim));
        return true;
        // } else {
        //   return false;
        //}
    }
    public boolean MoveLeft() {
        assert G != null;
        //int option = MapEmptyHood(G.divHood);
        //int option = MapHood(G.divHood);
        //if (option > 0) {
        //MoveSafeSQ(G.ItoX(place), G.ItoY(place));

        G.vecinity[Math.floorMod(G.ItoX(this.Isq())-1,G.yDim)][G.ItoY(this.Isq())]++;
        G.vecinity[G.ItoX(this.Isq())][G.ItoY(this.Isq())]--;
        MoveSQ( Math.floorMod(G.ItoX(this.Isq())-1,G.yDim),G.ItoY(this.Isq()));
        return true;
        // } else {
        //   return false;
        //}
    }
    public boolean MoveRight() {
        assert G != null;
        //int option = MapEmptyHood(G.divHood);
        //int option = MapHood(G.divHood);
        //if (option > 0) {
        //MoveSafeSQ(G.ItoX(place), G.ItoY(place));

        G.vecinity[ Math.floorMod(G.ItoX(this.Isq())+1,G.yDim)][G.ItoY(this.Isq())]++;
        G.vecinity[G.ItoX(this.Isq())][G.ItoY(this.Isq())]--;
        MoveSQ( Math.floorMod(G.ItoX(this.Isq())+1,G.yDim),G.ItoY(this.Isq()));
        return true;
        // } else {
        //   return false;
        //}
    }

    public boolean Reproduce(int time) {
        assert G != null;
        //int option = MapEmptyHood(G.divHood);
        int newindex = this.index;
        float newMutability = this.mutability, newStrength = this.strength;
        //if (option > 0) {
            if (mutability > G.rng.Double()) {
                if (.20f > G.rng.Double()) {
                    if (G.rng.Bool()) {
                        newStrength += G.rng.Gaussian(0, 5);
                        newStrength = Math.abs(newStrength);
                    } else {
                        newMutability += G.rng.Gaussian(0, 0.1);
                        float exponential = (float) Math.exp(-4.92*(newMutability-0.5f));
                        newMutability = 1/(1+exponential);
                    }
                    G.networks.get(G.FindIndex(newindex)).population++;
                    G.networks.get(G.FindIndex(newindex)).census++;
                } else {
                    newindex = ++G.maxindex;
                    G.networks.add(G.networks.get(G.FindIndex(this.index)).Mutate(newindex,strength,this.index,G.seed ,time));
                    G.Data.add(new DataStorage("Especie"+(G.maxindex )));

                }
            } else {
                G.networks.get(G.FindIndex(this.index)).population++;
                G.networks.get(G.FindIndex(this.index)).census++;
            }
            //int place = G.divHood[G.rng.Int(option)];
            int place=this.Isq();
            G.NewAgentSQ(place).Init(newindex, newMutability, newStrength);
            G.vecinity[G.ItoX(this.Isq())][G.ItoY(this.Isq())]++;
            return true;
        //} else {
          //  return false;
        //}
    }
}