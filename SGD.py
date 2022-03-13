import numpy as np


class SGD():
    def __init__(self, rank, maxIter, alpha = 0.001, beta = 0.05) -> None:
        self.rank = rank
        self.maxIter = maxIter
        self.alpha = alpha
        self.beta = beta
    
    def train(self, V):
        # V: [(i, j, v)]
        m = max([x[0] for x in V]) + 1
        n = max([x[1] for x in V]) + 1
        cnt = V.size

        W = np.array([np.ones(self.rank) for i in range(m)])
        H = np.array([np.zeros(self.rank) for i in range(n)])

        for i in range(self.maxIter):
            loss = self.update(V, W, H, self.alpha, self.beta, cnt)
            print("loss in iter %d = %f" % (i, loss))

        return W, H

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
    maxIter = 100
    
    ratings = np.array([(0, 0, 1), (0, 1, 3), (1, 1, 5)])
    
    sgd = SGD(rank, maxIter)
    W, H = sgd.train(ratings)

    print("=====finished=====")
