package com.fractala.core.models

/**
 * Configuration parameters for the L-System rendering and generation.
 *
 * @param lineLength The length of a single forward step.
 * @param lineWidth The initial width of the drawn lines.
 * @param turningAngle The default angle (in degrees) for left and right turns.
 * @param lineWidthIncrement The amount to add/subtract when changing line width.
 * @param lineWidthMultiplier The factor to multiply/divide by when scaling line width.
 * @param turningAngleIncrement The amount to add/subtract when changing the turning angle.
 * @param startingColor The initial color for drawing.
 */
case class Config(lineLength: Double,
                  lineWidth: Double,
                  turningAngle: Double,
                  lineWidthIncrement: Double,
                  lineWidthMultiplier: Double,
                  turningAngleIncrement: Double,
                  startingColor: Color);
