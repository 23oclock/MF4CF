import numpy as np
np.set_printoptions(precision=32)

filepath = "D:/codes/nmf/mat.csv"
A = np.loadtxt(filepath, delimiter=",")
print("-----A-----")
print(A)

print("-----norm(A)-----")
print(np.linalg.norm(A))

[Q, R] = np.linalg.qr(A)
print("-----R-----")
print(R)

Rinv = np.linalg.inv(R)
print("-----Rinv-----")
print(Rinv)