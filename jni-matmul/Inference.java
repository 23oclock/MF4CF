import java.util.Arrays;

public class Inference {
    static {
        try {
            System.loadLibrary("SimRank");
        } catch (Exception e) {
            System.out.println("load library failed!");
        }
    }

    public native void dsdgemm(int[] dimsAndIndices, double[] ab, double[] c);
    public native void sddgemm(int[] dimsAndIndices, double[] ba, double[] c);

    // dimsAndIndices = {m, n, k, aNNZ, aColPtrs, bColPtrs, aRowIndices, bRowInices}
    // ab = {aData, bData}
    public native CSCMatrix ssdgemm(int[] dimsAndIndices, double[] ab);

    public static void main(String[] args) {

        new Inference().dsdgemmTest();
        new Inference().ssdgemmTest();

    }

    public void dsdgemmTest() {
        System.out.println("--------dsdgemm---------");

        int m = 3;
        int k = 3;
        int n = 4;

        // 0 0 1 0
        // 1 0 0 0
        // 0 0 2 1

        double[] a = {
            1, 0, 0,
            0, 1, 0,
            0, 0, 1
        };
        double[] bValues = {1, 1, 2, 1};
        int[] bColPtrs = {0, 1, 1, 3, 4};
        int[] bRowIndices = {1, 0, 2, 2};
        double[] c = new double[m * n];

        int[] dimsAndIndices = new int[3+n+1+bRowIndices.length];
        dimsAndIndices[0] = m;
        dimsAndIndices[1] = n;
        dimsAndIndices[2] = k;
        System.arraycopy(bColPtrs, 0, dimsAndIndices, 3, bColPtrs.length);
        System.arraycopy(bRowIndices, 0, dimsAndIndices, 3+n+1, bRowIndices.length);

        double[] ab = new double[a.length + bValues.length];
        System.arraycopy(a, 0, ab, 0, a.length);
        System.arraycopy(bValues, 0, ab, a.length, bValues.length);

        dsdgemm(dimsAndIndices, ab, c);
        System.out.println(Arrays.toString(c));
    }

    public void ssdgemmTest() {
        System.out.println("--------ssdgemm---------");

        int m = 3;
        int k = 3;
        int n = 4;

        // double[] a = {
        //     1, 0, 0,
        //     0, 1, 0,
        //     0, 0, 1
        // };
        double[] aValues = {1, 1, 1};
        int[] aColPtrs = {0, 1, 2, 3};
        int[] aRowIndices = {0, 1, 2};

        // 0 0 1 0
        // 1 0 0 0
        // 0 0 2 1
        double[] bValues = {1, 1, 2, 1};
        int[] bColPtrs = {0, 1, 1, 3, 4};
        int[] bRowIndices = {1, 0, 2, 2};

        int[] dimsAndIndices = new int[3+1+k+1+n+1+aRowIndices.length+bRowIndices.length];
        dimsAndIndices[0] = m;
        dimsAndIndices[1] = n;
        dimsAndIndices[2] = k;
        dimsAndIndices[3] = aRowIndices.length;
        System.arraycopy(aColPtrs, 0, dimsAndIndices, 4, aColPtrs.length);
        System.arraycopy(bColPtrs, 0, dimsAndIndices, 4+aColPtrs.length, bColPtrs.length);
        System.arraycopy(aRowIndices, 0, dimsAndIndices, 4+aColPtrs.length+bColPtrs.length, aRowIndices.length);
        System.arraycopy(bRowIndices, 0, dimsAndIndices, 4+aColPtrs.length+bColPtrs.length+aRowIndices.length, bRowIndices.length);

        double[] ab = new double[aValues.length + bValues.length];
        System.arraycopy(aValues, 0, ab, 0, aValues.length);
        System.arraycopy(bValues, 0, ab, aValues.length, bValues.length);

        CSCMatrix csc = ssdgemm(dimsAndIndices, ab);
        System.out.println("return value:");
        System.out.println(Arrays.toString(csc.rowIndices));
        System.out.println(Arrays.toString(csc.colPtrs));
        System.out.println(Arrays.toString(csc.data));
    }

}