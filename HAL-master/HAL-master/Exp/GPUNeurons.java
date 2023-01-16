package Exp;

import org.lwjgl.opencl.*;
import HAL.Rand;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;


import java.nio.FloatBuffer;
import java.util.*;
import java.lang.System;

import static Exp.UtilCL.print;
import static org.lwjgl.opencl.CL10.*;




public class GPUNeurons {
    int[] ports ;
    float[] ProIput;
    int nLayer;
    int index;
    int population;
    int generation;
    int parentIndex;
    FloatBuffer answer;
    int census;
    float[] collected;
    ArrayList<FloatBuffer> layers;

    public static void main(String[] args) throws Exception{
        long seed=0;
        GPUNeurons neurons=new GPUNeurons(new int[] {3,3},0,seed);
        float[] ii=new float[3*2];
        ii=Randomize(seed,ii);
        FloatBuffer input=UtilCL.toFloatBuffer(ii);

        CL.create();

        CLPlatform platform = CLPlatform.getPlatforms().get(1);
        List<CLDevice> devices = platform.getDevices(CL_DEVICE_TYPE_GPU);
        CLContext context = CLContext.create(platform, devices, null, null, null);
        CLCommandQueue queue = clCreateCommandQueue(context, devices.get(0), CL_QUEUE_PROFILING_ENABLE, null);
        String source = "__kernel void "+
                "Evaluate(__global float* A,"+
                "          __global float* B,"+
                "          __global float* C,"+
                "          int wA, int wB)"+
                "{"+

                "int row = get_global_id(0);"+
                "int col = get_global_id(1);"+

                "float value = 0;"+
                "for (int k = 0; k < wA; ++k)"+
                "{"+
                "   float elementA = A[row*(wA+1)+k];"+
                "   float elementB = B[k*wB+col];"+
                "   value += elementA * elementB;"+
                "}"+
                "float sub=A[row*(wA+1)+wA];"+
                "C[wB*row+col] =2/(1+(exp(-value+sub)))-1;"+
                "}";


        CLProgram program = clCreateProgramWithSource(context, source, null);
        Util.checkCLError(clBuildProgram(program, devices.get(0), "", null));
        CLKernel Evaluate = clCreateKernel(program, "Evaluate", null);
        print(input);
        FloatBuffer answer= neurons.GPUCompute(context,queue,0,input,Evaluate);
        print(answer);
        clReleaseKernel(Evaluate);
        clReleaseProgram(program);
        clReleaseCommandQueue(queue);
        clReleaseContext(context);
        CL.destroy();
    }
    GPUNeurons(int[] ports,int index,long seed){
        this.ports=ports;
        this.answer=UtilCL.toFloatBuffer(new float[0]);
        this.index=index;
        this.population=1;
        this.generation=0;
        ProIput=new float[0];
        this.census=1;
        this.parentIndex=0;
        layers=new ArrayList<>();
        if(this.ports.length<2){
            System.out.print("no hay suficiente información para poder construir la red");
            System.exit(-1);
        }else{
            nLayer =this.ports.length-1;
            for (int layer = 0; layer < nLayer; layer++) {
                float[] floats=new float[ports[layer]*(ports[layer+1]+1)];
                floats=Randomize(seed,floats);
                layers.add(UtilCL.toFloatBuffer(floats));
            }

        }
    }
    static public float[] Randomize(long seed,float[] floats){
        Rand rng=new Rand(seed);
        for (int i = 0; i < floats.length; i++) {
            floats[i]=(float) rng.Gaussian(0,200);
        }
        return floats;
    }
    public FloatBuffer GPUCompute(CLContext context,CLCommandQueue queue,int layer,CLKernel Evaluate){
        FloatBuffer a=this.layers.get(layer);
        int NewRows=this.ports[layer+1];
        int common=this.ports[layer];
        FloatBuffer input=UtilCL.toFloatBuffer(this.ProIput);
        FloatBuffer answer=BufferUtils.createFloatBuffer((input.capacity()/common)*this.ports[layer+1]);
        //System.out.println(answer.capacity());
        CLMem aMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, a, null);
        clEnqueueWriteBuffer(queue, aMem, 1, 0, a, null, null);
        CLMem bMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, input, null);
        clEnqueueWriteBuffer(queue, bMem, 1, 0, input, null, null);
        CLMem answerMem = clCreateBuffer(context, CL_MEM_WRITE_ONLY | CL_MEM_COPY_HOST_PTR, answer, null);
        clFinish(queue);
        PointerBuffer kernel1DGlobalWorkSize = BufferUtils.createPointerBuffer(2);
        kernel1DGlobalWorkSize.put(0, NewRows);
        int NewColumns=input.capacity()/common;
        kernel1DGlobalWorkSize.put(1, NewColumns);
        Evaluate.setArg(0, aMem);
        Evaluate.setArg(1, bMem);
        Evaluate.setArg(2, answerMem);
        Evaluate.setArg(3,common);
        Evaluate.setArg(4,NewColumns);

        clEnqueueNDRangeKernel(queue, Evaluate, 2, null, kernel1DGlobalWorkSize, null, null, null);

        // Read the results memory back into our result buffer
        clEnqueueReadBuffer(queue, answerMem, 1, 0, answer, null, null);
        clFinish(queue);
        clReleaseMemObject(aMem);
        clReleaseMemObject(bMem);
        clReleaseMemObject(answerMem);
        if(layer!=this.nLayer-1) {
            return this.GPUCompute(context,queue,layer+1,answer,Evaluate);
        }else{
            return answer;
        }
    }
    public FloatBuffer GPUCompute(CLContext context,CLCommandQueue queue,int layer,FloatBuffer input,CLKernel Evaluate){
        FloatBuffer a=this.layers.get(layer);
        int NewRows=this.ports[layer+1];
        int common=this.ports[layer];

        FloatBuffer answer=BufferUtils.createFloatBuffer((input.capacity()/common)*this.ports[layer+1]);
        //System.out.println(answer.capacity());
        CLMem aMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, a, null);
        clEnqueueWriteBuffer(queue, aMem, 1, 0, a, null, null);
        CLMem bMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, input, null);
        clEnqueueWriteBuffer(queue, bMem, 1, 0, input, null, null);
        CLMem answerMem = clCreateBuffer(context, CL_MEM_WRITE_ONLY | CL_MEM_COPY_HOST_PTR, answer, null);
        clFinish(queue);
        PointerBuffer kernel1DGlobalWorkSize = BufferUtils.createPointerBuffer(2);
        kernel1DGlobalWorkSize.put(0, NewRows);
        int NewColumns=input.capacity()/common;
        kernel1DGlobalWorkSize.put(1, NewColumns);
        Evaluate.setArg(0, aMem);
        Evaluate.setArg(1, bMem);
        Evaluate.setArg(2, answerMem);
        Evaluate.setArg(3,common);
        Evaluate.setArg(4,NewColumns);

        clEnqueueNDRangeKernel(queue, Evaluate, 2, null, kernel1DGlobalWorkSize, null, null, null);

        // Read the results memory back into our result buffer
        clEnqueueReadBuffer(queue, answerMem, 1, 0, answer, null, null);
        clFinish(queue);
        clReleaseMemObject(aMem);
        clReleaseMemObject(bMem);
        clReleaseMemObject(answerMem);
        if(layer!=this.nLayer-1) {

            return this.GPUCompute(context,queue,layer+1,answer,Evaluate);
        }else{
            return answer;
        }
    }
    public GPUNeurons Mutate(int index,long seed,float strength){
        GPUNeurons newNeurons= new GPUNeurons(this.ports,index,seed);
        Rand rng=new Rand(seed);
        int length=0;
        for (int i = 0; i < layers.size(); i++) {
            length+=this.layers.get(i).capacity();
            newNeurons.layers.remove(i);
            newNeurons.layers.add(i,this.layers.get(i));
        }
        int thisOne =rng.Int(length)+1;

        length=0;
        for (int i = 0; i < layers.size(); i++) {
            if(thisOne<=length+layers.get(i).capacity()){
                thisOne-=length;
                float[] newlayer=new float[layers.get(i).capacity()];
                for (int j = 0; j < layers.get(i).capacity(); j++) {
                    newlayer[j]=layers.get(i).get(j);
                    if(j==thisOne){
                        newlayer[j]+=rng.Gaussian(0,strength);
                    }
                }
                newNeurons.layers.remove(i);
                newNeurons.layers.add(i, UtilCL.toFloatBuffer(newlayer));
                break;
            }
        }

        newNeurons.parentIndex=this.index;
        newNeurons.generation=this.generation+1;
        return newNeurons;
    }
}
