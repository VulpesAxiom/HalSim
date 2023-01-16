package Exp;


import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.*;


import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.lang.System;
import java.util.Random;
import Exp.Neural;
import static Exp.UtilCL.print;
import static org.lwjgl.opencl.CL10.*;

public class helloCL {

    // Data buffers to store the input and result data in

    //static final FloatBuffer a = UtilCL.toFlo

    public static void main(String[] args) throws Exception {
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
                "C[wB*row+col] =value-sub;"+
                "}";


        // Create our program and kernel
        CLProgram program = clCreateProgramWithSource(context, source, null);
        Util.checkCLError(clBuildProgram(program, devices.get(0), "", null));
        // sum has to match a kernel method name in the OpenCL source
        CLKernel Evaluate = clCreateKernel(program, "Evaluate", null);
        Random rn = new Random();
        int Length1 = 48;
        int Length2 = 30;
        int common=3;
        float[] A = new float[Length1] ;
        float[] B = new float[Length2] ;
        int NewColumns=(Length2/common);
        int NewRows=(Length1/(common+1));
        float[] C = new float[NewRows*NewColumns];
        for (int i = 0; i < Length1; i++) {
            A[i] = rn.nextFloat();
        }
        for (int i = 0; i < Length2; i++) {
            B[i]=rn.nextFloat();
        }
        //float[] A=new float[] {1,1,0,1,0,1,0,1,0,0,1,1};
        //float[] B=new float[] {2,3,4};
        Long time=System.currentTimeMillis();
        for (int row = 0; row < NewRows; row++) {
            for (int col= 0; col<NewColumns ; col++) {
                float value=0;
                for (int k = 0; k < common; k++) {
                    value+=A[row*(common+1)+k]*B[(NewColumns)*k+col];
                }
                C[row*NewColumns+col]=value-A[(row)*(common+1)+(common)];
            }
        }

        time=System.currentTimeMillis()-time;
        System.out.println("CPU: "+time);
        // Initialize OpenCL and create a context and command queue



        FloatBuffer a = UtilCL.toFloatBuffer(A);

        FloatBuffer b = UtilCL.toFloatBuffer(B);
        FloatBuffer answer = BufferUtils.createFloatBuffer(C.length);

        //displayInfo();


        // Allocate memory for our two input buffers and our result buffer
        answer=GPUCompute(context,queue,a,b,answer,Evaluate,NewRows,common);

        // Load the source from a resource file


        // Execution our kernel



        // Print the result memory
        print(answer);
        System.out.println("CPU: "+Arrays.toString(C));
        // Clean up OpenCL resources

        clReleaseKernel(Evaluate);
        clReleaseProgram(program);

        clReleaseCommandQueue(queue);
        clReleaseContext(context);
        CL.destroy();
    }
    static public FloatBuffer GPUCompute(CLContext context,CLCommandQueue queue,FloatBuffer a,FloatBuffer b,FloatBuffer answer,CLKernel Evaluate,int NewRows,int common){
        CLMem aMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, a, null);
        clEnqueueWriteBuffer(queue, aMem, 1, 0, a, null, null);
        CLMem bMem = clCreateBuffer(context, CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR, b, null);
        clEnqueueWriteBuffer(queue, bMem, 1, 0, b, null, null);
        CLMem answerMem = clCreateBuffer(context, CL_MEM_WRITE_ONLY | CL_MEM_COPY_HOST_PTR, answer, null);
        clFinish(queue);
        PointerBuffer kernel1DGlobalWorkSize = BufferUtils.createPointerBuffer(2);
        kernel1DGlobalWorkSize.put(0, NewRows);
        int NewColumns=b.capacity()/common;
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
        return answer;
    }
}