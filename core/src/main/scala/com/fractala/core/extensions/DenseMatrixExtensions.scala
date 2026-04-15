package com.fractala.core.extensions

import breeze.linalg.DenseMatrix
import breeze.numerics.sin

import scala.math.{Pi, cos}

extension (m: DenseMatrix.type)
  /** Creates a 2D rotation matrix from an angle in degrees.
    *
    * @param angleDeg
    *   The rotation angle in degrees.
    * @return
    *   A 2x2 DenseMatrix representing the rotation.
    */
  def rotation2DFromDegAngle(angleDeg: Double): DenseMatrix[Double] = {
    val angleRad = angleDeg / 180.0 * Pi
    val c = cos(angleRad)
    val s = sin(angleRad)

    DenseMatrix((c, -s), (s, c))
  }
