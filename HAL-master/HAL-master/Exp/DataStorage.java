package Exp;

import java.util.ArrayList;

import static Exp.ModeloEvolutivo.StoreLine;


public class DataStorage {
    String ID;
    ArrayList<InfoHolder> holder;
    ArrayList<float[]> diameter;
    DataStorage(String ID){
        this.ID=ID;
        holder=new ArrayList<>();
        diameter=new ArrayList<>();
    }
        public static class InfoHolder{
        String inputID;
        String eventID;
        String inputType;
        float diameterMean;
        float diameterDeviation;
        int choice;
        int action;
        InfoHolder(String input,String result,String type){
            inputID=input;
            eventID=result;
            inputType=type;
            diameterMean=0;
            diameterDeviation=0;
        }

    }
    public void AddData(ArrayList<String> Dictionary,float[] input,int choice,int action,float[] diameter){
        if(Dictionary.size()!=this.diameter.size()){
            for (int i = 0; i < Dictionary.size(); i++) {
                this.diameter.add(new float[3]);
            }
        }
        for (int i=0;i<Dictionary.size();i++) {
            ArrayList<InfoHolder> subholder=FindAllType(this.holder,Integer.toString(action),Float.toString(input[i]),Dictionary.get(i));
            if(subholder.size()<1){
                subholder.add(new InfoHolder(Float.toString(input[i]), Integer.toString(action), Dictionary.get(i)));
                this.holder.add(subholder.get(0));
            }
            subholder.get(0).action++;

            subholder=FindAllType(this.holder,Integer.toString(choice),Float.toString(input[i]),Dictionary.get(i));
            if(subholder.size()<1){
                subholder.add(new InfoHolder(Float.toString(input[i]), Integer.toString(choice), Dictionary.get(i)));
                this.holder.add(subholder.get(0));
            }
            subholder.get(0).choice++;
            this.diameter.get(i)[0]++;
            if(this.diameter.get(i)[0]==1){
                this.diameter.get(i)[1]=diameter[i];
                this.diameter.get(i)[2]=0;
            }else{
                float n=this.diameter.get(i)[0];
                float s=this.diameter.get(i)[2];
                float m=this.diameter.get(i)[1];
                this.diameter.get(i)[2]=(float) Math.sqrt((n-2)/(n-1)*s*s+(diameter[i]-m)*(diameter[i]-m)/n);
                this.diameter.get(i)[1]=((n-1)*m+diameter[i])/n;
            }
        }
    }
    static public DataStorage FindDS(ArrayList<DataStorage> Data, String ID){
        for (DataStorage data:Data){
            if (data.ID.equals(ID)) {
                return data;
            }
        }
        return null;
    }
    static public ArrayList<InfoHolder> FindInputType(ArrayList<InfoHolder> Data, String ID,String type){
        ArrayList<InfoHolder> list=new ArrayList<>();
        for (InfoHolder data:Data) {
            if(data.inputID.equals(ID) && data.inputType.equals(type)){
                list.add(data);
            }
        }
        return list;
    }
    static public ArrayList<InfoHolder> FindType(ArrayList<InfoHolder> Data, String type){
        ArrayList<InfoHolder> list=new ArrayList<>();
        for (InfoHolder data:Data) {
            if( data.inputType.equals(type)){
                list.add(data);
            }
        }
        return list;
    }
    static public ArrayList<InfoHolder> FindEventType(ArrayList<InfoHolder> Data, String ID,String type){
        ArrayList<InfoHolder> list=new ArrayList<>();
        for (InfoHolder data:Data) {
            if(data.eventID.equals(ID) && data.inputType.equals(type)){
                list.add(data);
            }
        }
        return list;
    }
    static public ArrayList<InfoHolder> FindAllType(ArrayList<InfoHolder> Data,String IDevent, String IDinput,String type){
        ArrayList<InfoHolder> list=new ArrayList<>();
        for (InfoHolder data:Data) {
            if(data.eventID.equals(IDevent) && data.inputID.equals(IDinput) &&data.inputType.equals(type)){
                list.add(data);
            }
        }
        return list;
    }
    static public String Collect(ArrayList<DataStorage> Data,String ID,ArrayList<String> Dictionary){
        ArrayList<InfoHolder> holders=FindDS(Data,ID).holder;
        StringBuilder counts= new StringBuilder(";");
        for (String s : Dictionary) {
            ArrayList<InfoHolder> subHolders = FindType(holders, s);
            counts.append(s).append(";");
            for (InfoHolder holder : subHolders) {
                counts.append(holder.inputID).append(";").append(holder.eventID).append(";").append(holder.action).append(";").append(holder.choice).append(";");
            }
        }
        return counts.toString();
    }
    static public void Delete(ArrayList<DataStorage> Data,String ID,ArrayList<String> Dictionary,String filepath){
        int i=0;
        while(!Data.get(i).ID.equals(ID)){
            i++;
        }

        int Cardinality=SumAction(Data.get(i).holder)/Dictionary.size();
        if(Cardinality!=0) {
            for (String type : Dictionary) {
                ArrayList<String> inputs = UniqueInput(FindType(Data.get(i).holder, type));
                ArrayList<String> outputs = UniqueEvent(FindType(Data.get(i).holder, type));
                float I=0;
                float H=0;
                for (String input : inputs) {
                    float Cx = (float) SumAction(FindInputType(Data.get(i).holder, input, type)) / Cardinality;
                    if (Cx != 0) {
                        for (String output : outputs) {
                            float Cy = (float) SumAction(FindEventType(Data.get(i).holder, output, type)) / Cardinality;
                            if (Cy != 0) {
                                float Cxy = (float) SumAction(FindAllType(Data.get(i).holder, output, input, type)) / Cardinality;
                                float Ratio = Cxy / (Cx * Cy);
                                if (Ratio != 0) {
                                    I += Cxy * Math.log(Ratio) / Math.log(2);
                                    H -= (Cxy * Math.log(Cxy)) / Math.log(2);

                                }
                            }
                        }
                    }
                }
                float MI = I / H;
                if (Float.isNaN(MI)) {
                    MI=0;
                }
                I=0;
                H=0;
                for (String input : inputs) {
                    float Cx = (float) SumChoice(FindInputType(Data.get(i).holder, input, type)) / Cardinality;
                    if (Cx != 0) {
                        for (String output : outputs) {
                            float Cy = (float) SumChoice(FindEventType(Data.get(i).holder, output, type)) / Cardinality;
                            if (Cy != 0) {
                                float Cxy = (float) SumChoice(FindAllType(Data.get(i).holder, output, input, type)) / Cardinality;
                                float Ratio = Cxy / (Cx * Cy);
                                if (Ratio != 0) {
                                    I += Cxy * Math.log(Ratio) / Math.log(2);
                                    H -= (Cxy * Math.log(Cxy)) / Math.log(2);

                                }
                            }
                        }
                    }
                }
                float mMI = I / H;
                if (Float.isNaN(mMI)) {
                    mMI=0;
                }
                StoreLine(filepath+type+".txt",ID+";"+Cardinality+";"+MI+";"+mMI+";"+Data.get(i).diameter.get(Dictionary.indexOf(type))[0]+";"+Data.get(i).diameter.get(Dictionary.indexOf(type))[1]+";"+Data.get(i).diameter.get(Dictionary.indexOf(type))[2]);
            }
        }
        Data.remove(i);
    }
    static public void Delete2(ArrayList<DataStorage> Data,String ID){
        int i=0;
        while(!Data.get(i).ID.equals(ID)){
            i++;
        }


        Data.remove(i);
    }
    static public ArrayList<InfoHolder> FindInput(ArrayList<InfoHolder> Data, String ID){
        ArrayList<InfoHolder> list=new ArrayList<>();
        for (InfoHolder data:Data) {
            if(data.inputID.equals(ID)){
                list.add(data);
            }
        }
        return list;
    }
    static public ArrayList<String > UniqueEvent(ArrayList<InfoHolder> Data){
        ArrayList<String> list=new ArrayList<>();
        for (InfoHolder data:Data) {
            if(!list.contains(data.eventID)){
                list.add(data.eventID);
            }
        }
        return list;
    }
    static public ArrayList<String > UniqueInput(ArrayList<InfoHolder> Data){
        ArrayList<String> list=new ArrayList<>();
        for (InfoHolder data:Data) {
            if(!list.contains(data.inputID)){
                list.add(data.inputID);
            }
        }
        return list;
    }
    static public int SumAction(ArrayList<InfoHolder> Data){
        int result=0;
        for (InfoHolder data:Data) {
            result+=data.action;
        }
        return result;
    }
    static public int SumChoice(ArrayList<InfoHolder> Data){
        int result=0;
        for (InfoHolder data:Data) {
            result+=data.choice;
        }
        return result;
    }
}
