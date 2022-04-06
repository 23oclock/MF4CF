package org.apache.spark.mllib.feature

import java.io.File

object TestQR {
  def main(args: Array[String]): Unit = {
    val m = 5
    val n = 3
    val mat = breeze.linalg.DenseMatrix.rand[Double](m, n)
    val lastCol = mat(::, mat.cols - 1).toDenseMatrix.t
    lastCol(0, 0) += 1e-12
    println(mat.rows, mat.cols, lastCol.rows, lastCol.cols)
    val A = breeze.linalg.DenseMatrix.horzcat(mat, lastCol)
//
    val filename = new File("./mat.csv")
    breeze.linalg.csvwrite(filename, A)

//    val A = breeze.linalg.csvread(filename)
    println("-----A-----")
    printFullMat(A)

    println("-----norm(A)-----")
    println(breeze.linalg.norm(A.toDenseVector))

    val R = breeze.linalg.qr.reduced(A).r
    println("-----R-----")
    printFullMat(R)

    val Rinv = breeze.linalg.inv(R)
    println("-----Rinv-----")
    printFullMat(Rinv)

    Thread.sleep(1000000)
  }

  def printFullMat(A: breeze.linalg.DenseMatrix[Double]) = {
    for (i <- 0 until A.rows) {
      println(A(i, ::).t)
    }
  }
}
