package Exp;

import HAL.GridsAndAgents.AgentGrid2D;
import HAL.GridsAndAgents.AgentSQ2D;
import HAL.Gui.GridWindow;
import HAL.Rand;
import HAL.Util;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.io.FileNotFoundException;


import java.lang.System;



public class EvolutionModel extends AgentGrid2D<Celula> {
    //Esta clase es la encargada de servir como medio ambiente para todas las simulaciones
    //filepath es el lugar donde los archivos se guardarán
    //seed es una semilla
    //Todos los boleanos son banderas de activación que cambia dependiendo de las necesidades de las simulaciones
    //Estas banderas se explicarán en main
    String filepath;
    long seed;
    boolean communicate,retrieve,special_comms,extended,memory;
    //maxSamples es la cantidad de muestras que cada genoma tomara
    //limits es el rango aceptable que pueden tomar los inputs
    int maxSamples;
    float [] limits;
    Rand rng;
    // estos maximos limitan la energia interna y la comida de cada simulación
    int maxInnerEnergy;
    int maximumFood;

    //Estas ArrayList contienen los genomas y el diccionario de los inputs
    ArrayList<Neural> networks = new ArrayList<>();
    ArrayList<String> Dictionary = new ArrayList<>();
    // AC es para manejar la aritmetica de matrices y otras cosas
    ArraysManagement AC = new ArraysManagement();
    //max index es el indice actual que ouede tomar un nuevo genoma
    //signaled_moved es la cantidad de agentes que se movieron y recivieron una señal
    //number of layers es la cantidad de capas que tiene la comunicación.
    int maxIndex,signaled_moved,number_of_layers;
    //estos arrays de 2 dimensiones son la cantidad de comida presente y la cantidad de agentes, en cada celda de la simulación
    int[][] food, neighborhood;
    // estos arrays son el registro del movimiento y la actividad
    float[] movement,activity;
    //Estos arrays son los ncargados de las señales buffer mantiene las señales por un tiempo de una iteracion de modo que
    //los agentes puedan escuchar sus propia señales
    int[][][] signal,signal_buffer;


    public EvolutionModel(int x, int y, int initial, int maxInnerEnergy, int iteration, long seed,boolean communicates,boolean retrieve,boolean special_comms,boolean extended,int number_of_layers,boolean memory) throws FileNotFoundException {
        super(x, y, Celula.class, true, true);
        //seed es la semilla que se utilizará para los electores aleatorios
        this.seed=seed;
        //movement es el un array, se divide en tres grupos de dos, el primer elemento de cada grupo es el movimiento en x
        //el segundo elemento es el movimiento en y
        //el primer gupo son todas las celulas
        //el segundo grupo son las celulas que detectaron señales
        // el tercer grupo son los que no detectarón señales.
        movement=new float[6];
        //mactivity es el un array, se divide en tres grupos de cinco, el primer elemento de cada grupo es la cantidad de
        // agentes de su grupo que se movio en una direcion determinada
        //el segundo elemento son los agnetes del grupo que se movieron en una direccion al azar
        //el tercer elemento son los que se reprodujeron
        //el cuarto grupo son los que realizaron apoptosis
        //el quinto grupo son la cantidad de agentes del grupo que murieon por causas naturales
        //el primer gupo son todas las celulas
        //el segundo grupo son las celulas que detectaron señales
        // el tercer grupo son los que no detectarón señales.
        activity=new float[15];
        //Estas son las banderas mencionadas en la clase
        this.memory=memory;
        this.extended=extended;
        this.number_of_layers=number_of_layers;
        signaled_moved=0;
        this.special_comms=special_comms;
        //Con la data entregada al constructor se determina el largo de los inputs
        int length_of_limits=12;
        if(extended){
            length_of_limits+=8;

        }
        if(communicates) {

            length_of_limits+=5*number_of_layers;
        }
        if(memory){
            length_of_limits++;
        }
        //una vez determinado el largo de los inputs, el largo de limits es el mismo
        limits=new float[length_of_limits];
        //En los siguientes pasos se determina el valor las entradas de limits
        limits[0]=10;
        int current=0;
        for (int i = 1; i <7 ; i++) {
            limits[i]=20;
            current=i+1;
        }
        for (int i = 0; i <5 ; i++) {
            limits[current+i]=5;
        }
        current+=5;
        if(extended){
            for (int i = 0; i < 8; i++) {
                limits[current+i]=5;
            }
            current+=8;
        }
        if(communicates){

            for (int i = 0; i < number_of_layers; i++) {
                for (int j = 0; j < 5; j++) {
                    limits[current+j]=1;
                }
                current+=5;
            }
        }
        if(memory){
            limits[current]=1;
        }
        this.communicate=communicates;
        if (retrieve) {
            maxSamples =0;
        }else{
            maxSamples=5000;
        }
        //Se escribe aqui donde deben guardarse los datos, para ello solo se cambia el string verde antes de iteration
        //antes de iteration debe de haber un slash o dos backslash dentro del string
        filepath = "C:\\Users\\bast_\\OneDrive\\Escritorio\\HalSim\\OutPutFile\\"+iteration+"/";
        //Si retrieve es verdadero el se rescatará el seed de "Description.txt" si no existe fallará la ejecucion de la simulación
        this.retrieve=retrieve;
        if(retrieve){
            File myObj = new File(filepath+"Description.txt");
            Scanner myReader = new Scanner(myObj);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                int place=data.indexOf(",");
                this.seed=Integer.parseInt(data.substring(8,place));
            }
        }
        //Se inicializan el resto de parametros
        rng=new Rand(this.seed);
        food = new int[xDim][yDim];
        neighborhood = new int[xDim][yDim];
        signal = new int[xDim][yDim][number_of_layers];
        signal_buffer= new int[xDim][yDim][number_of_layers];
        AC.ConstantThis(food, initial);
        this.maxInnerEnergy = maxInnerEnergy;
    }
    public int FindIndex(int index) {
        //Esta funcion encuentra la ubicacion del un genoma en el array de redes neuronales
        //Esto basado en el indice de la red neuronal.
        for (int i = 0; i < networks.size(); i++) {
            if (networks.get(i).index == index) {
                return i;
            }
        }
        System.out.println(index);

        //Si no lo encuentra enregará un -1 que eventualmente dará un error
        return -1;
    }
    static public void StoreLine(String filename, String lineToAppend,boolean delete) {
        //Esta funcion almacena en filename dentro de filepath la linea de texto lineToAppend
        //Se eliminan los archivos si es necesario
        if(delete){
            new File(filename).delete();
        }
        //Si no existe, el archivo es creado
        if (!new File(filename).exists()) {
            File file = new File(filename);
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //La linea es luego guardada
        try {
            FileWriter myWriter = new FileWriter(filename, true);
            myWriter.write(lineToAppend + "\n");
            myWriter.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
    public void Setup(int maxIndex,int initFood,float Strength,int[] architecture,float mutability) {
        //Esta rutina solo pone a los agentes iniciales en sus casillas escogidas al azar y randomiza las redes de cada uno
        //de los genomas
        for (int i = 1; i <= maxIndex; i++) {
            NewAgentSQ( rng.Int(xDim), rng.Int(yDim)).Init(i, mutability, Strength,initFood);
            networks.add(new Neural(architecture,i));
            networks.get(i-1).Randomize(rng.Long(2000));
            this.maxIndex =maxIndex;
        }
    }


    public void Feed(int amount, int frequency){
        //Esta funcion crea más comida en el mapa y actualiza la comunicacion
        //Recorre celda por celda
        for (int x = 0; x < xDim; x++) {
            for (int y = 0; y < yDim; y++) {
                //Crea una copia de la comida en la simulacion
                int[][] food2=food;
                //Determina las celdas vecinas a la actual
                int up=Math.floorMod(y+1, yDim);
                int down=Math.floorMod(y-1 ,yDim);
                int left= Math.floorMod(x-1 ,xDim);
                int right= Math.floorMod(x+1 ,xDim);
                //Calcula la suma de esta vecindad para así calcular la probabilidad de que aumente la food en la
                //Casilla actual
                int sum=food2[x][up]+food2[x][down]+food2[left][y]+food2[right][y]+food2[x][y];
                if(rng.Double()<((float)frequency)/(xDim*yDim)*Math.exp(-Math.pow(((float)sum/(maximumFood)-2.5f),2))) {
                    food[x][y] = Math.min((food[x][y] + amount), maximumFood);
                }
                //Tras actualizar la comida, si la comunicacion está habilitada se limpiaran los mensajes muy viejos y
                // se guardaran los de la interacion anterior si es que special_comms es verdadera
                if(communicate) {
                    for (int i = 0; i < number_of_layers; i++) {
                        if (special_comms) {
                            signal_buffer[x][y][i] = signal[x][y][i];
                        }
                        signal[x][y][i] = 0;
                    }
                }
            }
        }
    }
    public void Draw(GridWindow win,int max) {
        //Está funcion dibuja el estado actual de la simulación
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
                if(signal[x][y][0]+signal_buffer[x][y][0]>0) {
                    win.SetPix(i, Util.RGB(1, 0, 0));
                }else{
                    win.SetPix(i, Util.RGB(c, c, 0));
                }
            }
        }
    }


    public void CheckPop(){
        //Esta funcion le actualiza la cantidad de agentes en cada celda
        for (int i = 0; i < xDim; i++) {
            for (int j = 0; j < yDim; j++) {
                neighborhood[i][j]=this.PopAt(i,j);
            }
        }
    }
    public boolean CheckPop2(){
        //Esta funcion se asegura de que haya al menos un agente y entrega
        //un booleano veradero si es así y falso si no

        for (int i = 0; i < xDim; i++) {
            for (int j = 0; j < yDim; j++) {
                if(this.PopAt(i,j)>0){
                    return true;
                }
            }
        }
        return false;
    }

    public static void main(String[] args) throws Exception {
        //La simulacion vive aqui, este es el main
        Rand rng=new Rand(564L);
        int iteration =500,last_iteration=500;
        // Fuerza de mutacion
        float MStrength=10;
        // Probabilidad de mutacion
        float mutability=0.16f;
        // numero de agentes iniciales
        int initialNumber=250;
        // se sofocan los agentes cundo hay mucho s muy juntos?
        boolean Suffocate=true;
        // Probabilidad maxima de produccion de comida
        float subFreq=.05f;
        // Cuantas iteraciones tiene una simulacion
        int Duration=10000;
        // to draw decide si se dibuja o no la simulacion, memory si los agentes tienen memoria, communicates decide si se comunican
        // retrieve es para recrear una simulacion ya hecha, signal resistance es para que los agentes puedan detectar sus propias señales
        // extended determina si se usa la vecindad extendida
        boolean to_draw=true,memory=false,communicates=true,retrieve=true,signal_resistance=true,extended =false;
        // el numero de bits de comunicación
        int number_of_layers=1;

        //parte en la iteración inicial y termina en la final
        while (iteration <= last_iteration ) {
            int time = 0;
            int Pop;
            int xDim=300 , yDim=300;
            int BeginnerBoost=5;
            int[] architecture;
            int density = 1;
            //Se calcula la frecuencia de repocición de comida
            int frequency = (int) ((subFreq * xDim * yDim) / density);
            int scale = 600 / Math.max(xDim, yDim);
            // se crea la ventana y se cierra si to_draw está desactivado
            GridWindow win = new GridWindow( "",xDim, yDim, scale,to_draw,null,true);
            if(!to_draw){
                win.Close();
            }
            //initial es la cantidad inicial de energia en los agentes
            int initial = 10;
            int maxima = 10;
            long seed;
            //Se determina si la semilla se repite o se elige una al azar basada en la semilla más arriba
            if(retrieve) {
                seed = 1615135L;
            }else {
                seed = rng.Long(1000);
            }
            //Se inicializa el modelo
            EvolutionModel model = new EvolutionModel(xDim, yDim, initial, maxima,iteration,seed,communicates,retrieve,signal_resistance,extended,number_of_layers,memory);
            File directory = new File(model.filepath);
            // Se necesita ver si existe el directorio, si no, se intenta crear
            if (! directory.exists()){
                boolean done=false;
                while(!done){
                    done=directory.mkdir();
                }
            }
            //si retrieve es falso se escribe una descripcion general de la simulación
            if(!retrieve) {
                StoreLine(model.filepath + "Description.txt", "Semilla:" + model.seed + ", xDim:" + xDim + ", yDim:" + yDim,false);
            }
            model.maximumFood = 10;
            //Se genera el diccionario
            model.Dictionary.add("Random");
            model.Dictionary.add("Energy");
            model.Dictionary.add("UnderFood");
            model.Dictionary.add("NorthFood");
            model.Dictionary.add("SouthFood");
            model.Dictionary.add("WestFood");
            model.Dictionary.add("EastFood");
            model.Dictionary.add("UnderNeigh");
            model.Dictionary.add("NorthNeigh");
            model.Dictionary.add("SouthNeigh");
            model.Dictionary.add("WestNeigh");
            model.Dictionary.add("EastNeigh");
            if(extended){
                model.Dictionary.add("ExNorthNeigh");
                model.Dictionary.add("ExSouthNeigh");
                model.Dictionary.add("ExWestNeigh");
                model.Dictionary.add("ExEastNeigh");
                model.Dictionary.add("Diagonal1");
                model.Dictionary.add("Diagonal2");
                model.Dictionary.add("Diagonal3");
                model.Dictionary.add("Diagonal3");
            }

            if(communicates) {

                model.Dictionary.add("UnderSignal");
                model.Dictionary.add("NorthSignal");
                model.Dictionary.add("SouthSignal");
                model.Dictionary.add("WestSignal");
                model.Dictionary.add("EastSignal");
                for(int j=1;j < number_of_layers;j++){
                    model.Dictionary.add("UnderSignal"+j+1);
                    model.Dictionary.add("NorthSignal"+j+1);
                    model.Dictionary.add("SouthSignal"+j+1);
                    model.Dictionary.add("WestSignal"+j+1);
                    model.Dictionary.add("EastSignal"+j+1);
                }

            }else{
                number_of_layers=0;
            }
            if(memory){
                model.Dictionary.add("Memory");
                number_of_layers+=1;
            }
            //Se genera la arquitectura de lkas redes neuronales
            architecture=new int[]{model.Dictionary.size(),25,7 +number_of_layers};
            if(memory){
                number_of_layers--;
            }
            //Con lo ya inicializado se inicia Setup
            model.Setup(initialNumber,BeginnerBoost,MStrength,architecture,mutability);
            // Se registra el numero inicial de agentes
            model.CheckPop();
            // Estas variables son para el reloj de la consola
            long starting = System.currentTimeMillis();
            long minute;
            long hour;
            long second;
            int population=0;

            while (model.CheckPop2() && (time < Duration)) {
                //Se recorre iteracion por iteracion, hasta la iteracion maxima o se acaban los agentes
                second = ((System.currentTimeMillis() - starting) / 1000) % 60;
                minute = ((System.currentTimeMillis() - starting) / 60000) % 60;
                hour = ((System.currentTimeMillis() - starting) / 3600000);

                //Se randomiza el orden de los agentes para evitar sesgos
                model.ShuffleAgents(model.rng);
                Pop=model.Pop();
                population=0;
                for (Celula cell : model) {
                    //Todas las celulas se activan en este orden utilizando su metodo "Step"
                    cell.Step(time,Suffocate);
                    //Se registra la poblacion que ha interactuado durante esta iteracion
                    population++;
                }
                if(!retrieve) {
                    //cuando retrieve es falso se generan las muestras de aquellos genomas extintos
                    for (int iterante = model.networks.size() - 1; iterante >= 0; iterante--) {
                        //Se recorren todos los genomas
                        Neural net = model.networks.get(iterante);
                        if (net.Extinct && net.Samples < model.maxSamples) {
                            //Si el genoma está extinta y no tiene suficientes muestras se toma una muestra
                            for (int contador = 0; contador < 10; contador++) {
                                // 10 mustras se toman a no ser que se terminen las muestras
                                net.Sample(model.limits, model.communicate,model.extended, model.number_of_layers,model.memory);
                                if (net.Samples >= model.maxSamples) {
                                    net.StoreSample(model.limits, model.filepath);
                                    model.networks.remove(model.FindIndex(net.index));
                                    break;
                                }
                            }
                        } else if (net.Extinct) {
                            //Si el genoma en cuestion está extinto y con todas sus muestras se elimina de la lista
                            net.StoreSample(model.limits, model.filepath);
                            model.networks.remove(model.FindIndex(net.index));
                        }
                    }
                }
                // Se aumenta la cantidad de comida y se actualizan las señales
                model.Feed(density,frequency);
                if(to_draw) {
                    //Se dibuja
                    model.Draw(win, model.maximumFood);
                    win.TickPause(50);
                }

                System.out.println(hour + ":" + minute + ":" + second + "  ;" + time+ "  ;" +Pop);
                //Se escribe el reloj en consola
                boolean delete=false;
                if(time==0){
                    delete=true;
                }
                StoreLine(model.filepath+"Populations.txt",time+";"+population+";"+ model.signaled_moved+";"+(population-model.signaled_moved),delete);
                //Se determina primero la direccion promedio en la que se mueven los agentes esta iteracion
                model.movement[0] /=population;
                model.movement[1] /=population;
                model.movement[2] /= model.signaled_moved;
                model.movement[3] /=model.signaled_moved;
                model.movement[4] /=(population-model.signaled_moved);
                model.movement[5] /=(population-model.signaled_moved);
                //Se almacena esta info en un archivo de texto
                StoreLine(model.filepath+"Movement.txt",time+";"+model.AC.ToString(model.movement),delete);
                for(int i=0;i<5;i++){
                    model.activity[i]/=population;
                    model.activity[i+5]/=model.signaled_moved;
                    model.activity[i+10]/=(population-model.signaled_moved);
                }
                //Similarmente se determina el promedio de acciones que toman los agentes esta iteración
                StoreLine(model.filepath+"Activity.txt",time+";"+model.AC.ToString(model.activity),delete);
                //Se almacena esto en un archivo de texto
                model.signaled_moved=0;
                model.movement=new float[6];
                model.activity=new float[15];
                ++time;
            }
            //Una vez se terminan las iteraciones de la simulaciones se matan todas las celulas
            for (Celula cell: model ) {
                cell.Die(time);
            }
            // Se recorren todos los genomas y se hacen las muestras necesarias antes de terminar la simulación
            if (!retrieve) {
                for (int iterante = model.networks.size() - 1; iterante >= 0; iterante--) {
                    Neural net = model.networks.get(iterante);
                    while (net.Samples < model.maxSamples) {
                        net.Sample(model.limits, model.communicate,model.extended,model.number_of_layers,model.memory);
                    }

                    net.StoreSample(model.limits, model.filepath);

                    model.networks.remove(model.FindIndex(net.index));

                }
            }
            iteration++;
        }
        System.exit(100);
    }

}

//Esta es la clase de las células, cuentan con un index, que corresponde al índice del genoma que poseen
// energy que es la energía que poseen

class Celula extends AgentSQ2D<EvolutionModel> {
    int index;
    int energy;
    int memory;
    boolean any_signal;
    float mutability, strength;
    int order;
    //Esta función inicializa las propiedades de la célula (en este caso una descendiente de las originales)
    public void Init(int index, float mutability, float strength) {
        energy = 1;
        memory=0;
        order=-1;
        this.index = index;
        this.strength = strength;
        this.mutability = mutability;
    }
    //Esta función inicializa las propiedades de la célula (en este caso una de las iniciales)
    public void Init(int index, float mutability, float strength,int food) {
        energy = food;
        order=-1;
        this.index = index;
        this.strength = strength;
        this.mutability = mutability;
    }

    public void Step(int time,boolean suffocates) {

        assert G != null;
        int bound = 10;
        float rng = (float) G.rng.Int(bound);

        //Se determinan las direcciones de la vecindad de la celula
        int up=Math.floorMod(G.ItoY(this.Isq())-1,G.yDim);
        int down=Math.floorMod((G.ItoY(this.Isq())+1),G.yDim);
        int thisx=G.ItoX(this.Isq());
        int thisy=G.ItoY(this.Isq());
        int left=(Math.floorMod((G.ItoX(this.Isq())-1),G.xDim));
        int right=(Math.floorMod((G.ItoX(this.Isq())+1),G.xDim));
        //Se determina el largo del vector de inputs
        int length_of_input=12;
        if(G.communicate){
            length_of_input+=5*G.number_of_layers;
        }
        if(G.extended){
            length_of_input+=8;
        }
        if(G.memory){
            length_of_input++;
        }
        //En las lineas siguientes se pobla un vector "input" con la información pertinente
        float[] input= new float[length_of_input];
        input[0]=rng;
        input[1]=this.energy;
        input[2]=G.food[thisx][up];
        input[3]=G.food[left][thisy];
        input[4]=G.food[right][thisy];
        input[5]=G.food[thisx][down];
        input[6]=G.food[thisx][thisy];

        input[7]= G.neighborhood[thisx][up];
        input[8]= G.neighborhood[thisx][thisy];
        input[9]= G.neighborhood[thisx][down];
        input[10]= G.neighborhood[left][thisy];
        input[11]= G.neighborhood[right][thisy];
        int this_neigh= (int) (input[7]+input[8]+input[9]+input[10]+input[11]);
        int current =12;
        if(G.extended){
            int exup = Math.floorMod(G.ItoY(this.Isq()) - 2, G.yDim);
            int exdown = Math.floorMod((G.ItoY(this.Isq()) + 2), G.yDim);
            int exleft = (Math.floorMod((G.ItoX(this.Isq()) - 2), G.xDim));
            int exright = (Math.floorMod((G.ItoX(this.Isq()) + 2), G.xDim));
            input[current] = G.neighborhood[thisx][exup];
            input[current+1]= G.neighborhood[thisx][exdown];
            input[current+2]= G.neighborhood[exleft][thisy];
            input[current+3]= G.neighborhood[exright][thisy];
            input[current+4]= G.neighborhood[right][up];
            input[current+5]= G.neighborhood[left][down];
            input[current+6]= G.neighborhood[left][up];
            input[current+7]= G.neighborhood[right][down];
            current+=5;

        }
        this.any_signal=false;
        if(G.communicate){
            for (int i = 0; i < G.number_of_layers; i++) {
                input[current]=Math.max(G.signal[thisx][up][0],G.signal_buffer[thisx][up][i]);
                input[current+1] = Math.max(G.signal[thisx][thisy][0],G.signal_buffer[thisx][thisy][i]);
                input[current+2] = Math.max(G.signal[thisx][down][0],G.signal_buffer[thisx][down][i]);
                input[current+3] = Math.max(G.signal[left][thisy][0],G.signal_buffer[left][thisy][i]);
                input[current+4] = Math.max(G.signal[right][thisy][0],G.signal_buffer[right][thisy][i]);
                if((input[current]!=0 ||input[current+1]!=0 ||input[current+2]!=0 || input[current+3]!=0 || input[current+4]!=0)&& !this.any_signal){
                        G.signaled_moved++;
                        this.any_signal = true;
                }
                current+=5;
            }
        }
        if(G.memory){
            input[current]=this.memory;
        }

        //Se computa el output
        float[] output = G.networks.get(G.FindIndex(index)).Compute(input, 1);
        if(G.networks.get(G.FindIndex(index)).Samples < G.maxSamples && !G.retrieve) {
            //Se toma una muestra
            G.networks.get(G.FindIndex(index)).Sample(G.limits,G.communicate,G.extended,G.number_of_layers,G.memory);
        }
        int responce = G.AC.getIndexOfLargest(output,7);
        if (output[responce] < 0) {
            responce = 3;
        }
        if(G.communicate) {
            //Si hay comunicación se almacena en la vecindad dependiendo de la cantidad de bits
            for (int i = 0; i <G.number_of_layers ; i++) {
                if (output[7+i] > 0.5) {
                    output[7+i] = 1;
                } else {
                    output[7+i] = 0;
                }
                G.signal[thisx][thisy][i] = (int) output[7+i];
                G.signal[left][thisy][i] = (int) output[7+i];
                G.signal[right][thisy][i] = (int) output[7+i];
                G.signal[thisx][up][i] = (int) output[7+i];
                G.signal[thisx][down][i] = (int) output[7+i];
            }
        }
        if(G.memory){
            //Si hay memoria se almacena el ultimo output como memoria
            if (output[output.length-1] > 0.5) {
                output[output.length-1] = 1;
            } else {
                output[output.length-1] = 0;
            }
            this.memory= (int) output[output.length-1];
        }

        boolean dies=responce==1;
        if(dies){
            //apoptosis
            G.activity[3]++;
            if(any_signal){
                G.activity[8]++;
            }else{
                G.activity[13]++;
            }
        }
        this.energy--;
        if (this.energy < 0) {
            //Se detecta si murió por falta de energía
            if(!dies) {
                G.activity[4]++;
                if (any_signal) {
                    G.activity[9]++;
                } else {
                    G.activity[14]++;
                }
                dies = true;
            }

        } else {
            switch (responce) {
                //Se decide la acción y se registra en activity
                case 0 -> {
                    if (this.energy >= 2) {
                        if (Reproduce(time)) {
                            this.energy -= 2;
                            G.activity[2]++;
                            if(any_signal){
                                G.activity[7]++;
                            }else{
                                G.activity[12]++;
                            }
                        }
                    }
                }
                case 2 -> {
                    if (this.energy >= 1) {
                        if (MoveRandom()) {
                            this.energy--;
                            G.activity[1]++;
                            if(any_signal){
                                G.activity[6]++;
                            }else{
                                G.activity[11]++;
                            }
                        }
                    }
                }
                case 3 -> {
                    if (this.energy >= 1) {
                        if (MoveUp()) {
                            this.energy--;
                            G.activity[0]++;
                            if(any_signal){
                                G.activity[5]++;
                            }else{
                                G.activity[10]++;
                            }
                        }
                    }
                }
                case 4 -> {
                    if (this.energy >= 1) {
                        if (MoveDown()) {
                            this.energy--;
                            G.activity[0]++;
                            if(any_signal){
                                G.activity[5]++;
                            }else{
                                G.activity[10]++;
                            }
                        }
                    }
                }
                case 5 -> {
                    if (this.energy >= 1) {
                        if (MoveRight()) {
                            this.energy--;
                            G.activity[0]++;
                            if(any_signal){
                                G.activity[5]++;
                            }else{
                                G.activity[10]++;
                            }
                        }
                    }
                }
                case 6 -> {
                    if (this.energy >= 1) {
                        if (MoveLeft()) {
                            this.energy--;
                            G.activity[0]++;
                            if(any_signal){
                                G.activity[5]++;
                            }else{
                                G.activity[10]++;
                            }
                        }
                    }
                }
            }
        }


        if (this.energy < 0) {
            //Se evita la energía negativa
            this.energy = 0;
        }
        if ((suffocates && this_neigh>5) || dies) {
            //Si se sofoca, se queda sin energía o si decide morir, el agente muere
            if((suffocates && this_neigh>5) && !dies){
                G.activity[4]++;
                if(any_signal){
                    G.activity[9]++;
                }else{
                    G.activity[14]++;
                }
            }
            Die(time);
        }
        //La célula se alimenta
        if (this.energy < G.maxInnerEnergy) {
            int remnant = G.maxInnerEnergy - energy;
            if (remnant <= G.food[G.ItoX(Isq())][G.ItoY(Isq())]) {
                G.food[G.ItoX(Isq())][G.ItoY(Isq())] -= remnant;
                this.energy += remnant;
            } else {
                this.energy += G.food[G.ItoX(Isq())][G.ItoY(Isq())];
                G.food[G.ItoX(Isq())][G.ItoY(Isq())] = 0;
            }
        }
    }
    public void Die(int time) {
        // Esta función mata al agente
        assert G != null;
        G.food[G.ItoX(Isq())][G.ItoY(Isq())] += Math.max(0, energy);
        G.neighborhood[G.ItoX(Isq())][G.ItoY(Isq())]--;

        Dispose();
        G.networks.get(G.FindIndex(index)).population--;
        if (G.networks.get(G.FindIndex(index)).population == 0) {
            G.networks.get(G.FindIndex(index)).Extinct = true;
            G.networks.get(G.FindIndex(index)).died=time;
        }
    }

    public boolean MoveRandom() {
        //Esta función elege al azar una dirección a la cual el agente se mueve
        assert G != null;
        int option =G.rng.Int(4);
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
    }
    public boolean MoveUp() {
        //Esta función mueve al agente hacia arriba
        assert G != null;
        G.neighborhood[G.ItoX(this.Isq())][Math.floorMod(G.ItoY(this.Isq())-1,G.yDim)]++;
        G.neighborhood[G.ItoX(this.Isq())][G.ItoY(this.Isq())]--;
        MoveSQ(G.ItoX(this.Isq()), Math.floorMod(G.ItoY(this.Isq())-1,G.yDim));
        G.movement[1]--;
        if(any_signal){
            G.movement[3]--;
        }else{
            G.movement[5]--;
        }
        return true;
    }
    public boolean MoveDown() {
        //Esta función mueve al agente hacia abajo
        assert G != null;
        G.neighborhood[G.ItoX(this.Isq())][Math.floorMod(G.ItoY(this.Isq())+1,G.yDim)]++;
        G.neighborhood[G.ItoX(this.Isq())][G.ItoY(this.Isq())]--;
        MoveSQ(G.ItoX(this.Isq()), Math.floorMod(G.ItoY(this.Isq())+1,G.yDim));
        G.movement[1]++;
        if(any_signal){
            G.movement[3]++;
        }else{
            G.movement[5]++;
        }
        return true;
    }
    public boolean MoveLeft() {
        //Esta función mueve al agente hacia la izquierda
        assert G != null;
        G.neighborhood[Math.floorMod(G.ItoX(this.Isq())-1,G.yDim)][G.ItoY(this.Isq())]++;
        G.neighborhood[G.ItoX(this.Isq())][G.ItoY(this.Isq())]--;
        MoveSQ( Math.floorMod(G.ItoX(this.Isq())-1,G.yDim),G.ItoY(this.Isq()));
        G.movement[0]--;
        if(any_signal){
            G.movement[2]--;
        }else{
            G.movement[4]--;
        }
        return true;
    }
    public boolean MoveRight() {
        //Esta función mueve al agente hacia la derecha
        assert G != null;
        G.neighborhood[ Math.floorMod(G.ItoX(this.Isq())+1,G.yDim)][G.ItoY(this.Isq())]++;
        G.neighborhood[G.ItoX(this.Isq())][G.ItoY(this.Isq())]--;
        MoveSQ( Math.floorMod(G.ItoX(this.Isq())+1,G.yDim),G.ItoY(this.Isq()));
        G.movement[0]++;
        if(any_signal){
            G.movement[2]++;
        }else{
            G.movement[4]++;
        }
        return true;
    }

    public boolean Reproduce(int time) {
        // Esta función reproduce al agente y se encarga de ver si muta el genoma y si muta llama
        // a las funciones necesarias para las mutaciones
        assert G != null;
        int newindex = this.index;
        float newMutability = this.mutability, newStrength = this.strength;

        if (mutability > G.rng.Double()) {
            if (.20f > G.rng.Double()) {
                if (G.rng.Bool()) {
                    newStrength += G.rng.Gaussian(0, 5);
                    newStrength = Math.abs(newStrength);
                } else {
                    newMutability += G.rng.Gaussian(0, 0.1);
                    float exponential = (float) Math.exp(-4.92 * (newMutability - 0.5f));
                    newMutability = 1 / (1 + exponential);
                }
                G.networks.get(G.FindIndex(newindex)).population++;
            } else {
                newindex = ++G.maxIndex;
                G.networks.add(G.networks.get(G.FindIndex(this.index)).Mutate(newindex, strength, this.index, G.seed, time));
            }
        } else {
            G.networks.get(G.FindIndex(this.index)).population++;
        }
        int place = this.Isq();
        G.NewAgentSQ(place).Init(newindex, newMutability, newStrength);
        G.neighborhood[G.ItoX(this.Isq())][G.ItoY(this.Isq())]++;

        return true;
    }
}