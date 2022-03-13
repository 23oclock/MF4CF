import numpy as np
from scipy import sparse


class CCD():
    def __init__(self, rank, max_iter, inner_iter, alpha) -> None:
        self.rank = rank
        self.max_iter = max_iter
        self.inner_iter = inner_iter
        self.alpha = alpha
    
    def train(self, V):
        # V: [(i, j, v)]
        m = max([x[0] for x in V]) + 1
        n = max([x[1] for x in V]) + 1
        cnt = V.size
        R = sparse.coo_matrix(([x[2] for x in V], ([x[0] for x in V], [x[1] for x in V])), (m, n)).tocsc()
        RT = sparse.coo_matrix(([x[2] for x in V], ([x[1] for x in V], [x[0] for x in V])), (m, n)).tocsc()
        print(R)
        print(RT)

        W = [[1 for _ in range(m)] for i in range(self.rank)]
        H = [[0 for _ in range(n)] for i in range(self.rank)]

        for oiter in range(self.max_iter):
            for t in range(self.rank):
                u = W[t]
                v = H[t]
                R = self.updateR(R, u, v, False)
                RT = self.updateR(RT, v, u, False)
                print(R)
                print("---")
                for iiter in range(self.inner_iter):
                    for j in range(n):
                        v[j] = self.rank_one_update(R, j, u, self.alpha)
                    for i in range(m):
                        u[i] = self.rank_one_update(RT, i, v, self.alpha)
                R = self.updateR(R, u, v, True)
                RT = self.updateR(RT, v, u, True)
        return W, H

    def rank_one_update(self, R, j, u, alpha):
        if (R.indptr[j] == R.indptr[j + 1]):
            return 0
        g = 0
        h = alpha * (R.indptr[j + 1] - R.indptr[j])
        for idx in range(R.indptr[j], R.indptr[j + 1]):
            i = R.indices[idx]
            g += R.data[idx] * u[i]
            h += u[i] * u[i]
        return g / h if g > 0 else 0

    def updateR(self, R, u, v, add):
        data = R.data.copy()
        for j in range(R.shape[1]):
            start = R.indptr[j]
            end = R.indptr[j + 1]
            for idx in range(start, end):
                i = R.indices[idx]
                uv = u[i] * v[j]
                if add:
                    data[idx] += uv
                else:
                    data[idx] -= uv
        return sparse.csc_matrix((data, R.indices, R.indptr), R.shape)



    def update(self, V, W, H, alpha, beta, cnt):
        error = 0
        for (i, j, v) in V:
            loss = v - W[i] @ H[j]
            error += loss * loss
            W[i] += alpha * (loss * H[j] - beta * W[i])
            H[j] += alpha * (loss * W[i] - beta * H[j])
        return loss / cnt


if __name__ == "__main__":
    print("=====start=====")
    
    rank = 10
    max_iter = 100
    inner_iter = 5
    alpha = 0.1
    
    ratings = np.array([(0, 0, 1), (0, 1, 3), (1, 1, 5)])
    
    sgd = CCD(rank, max_iter, inner_iter, alpha)
    W, H = sgd.train(ratings)

    print("=====finished=====")
