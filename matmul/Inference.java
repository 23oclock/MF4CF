import java.util.Arrays;

public class Inference {
    static {
        try {
            System.loadLibrary("SimRank");
        } catch (Exception e) {
            System.out.println("load library failed!");
        }
    }

    private native void dddgemm(int[] dims, double[] ab, double[] c);
    private native void dsdgemm(int m, int n, int k, double[] a, double[] values, int[] colPtrs, int[] rowIndices, double[] c);

    public static void main(String[] args) {

        new Inference().dddgemmTest();
        new Inference().dsdgemmTest();

    }

    public void dddgemmTest() {
        System.out.println("--------dddgemm---------");

        int[] dims = {2, 2, 2};
        double[] ab = {1, 2, 3, 4, 5, 6, 7, 8};
        double[] c = {0, 0, 0, 0};
        new Inference().dddgemm(dims, ab, c);
        System.out.println(Arrays.toString(c));     
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
        double[] values = {1, 1, 2, 1};
        int[] colPtrs = {0, 1, 1, 3, 4};
        int[] rowIndices = {1, 0, 2, 2};
        double[] c = new double[12];

        dsdgemm(m, n, k, a, values, colPtrs, rowIndices, c);
        System.out.println(Arrays.toString(c));
    }
}