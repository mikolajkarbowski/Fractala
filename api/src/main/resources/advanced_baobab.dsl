Config {
  lineLength: 40.0
  lineWidth: 2.0
  turningAngle: 25.0
  lineLengthMultiplier: 0.75
  lineWidthIncrement: 1.2
  maxIterations: 6
  startingColor: bark
}

Colors {
  bark: 0.45, 0.30, 0.15
  leaves: 0.25, 0.65, 0.30
}

Axiom: X

Rules {
  X -> <bark> F [ + X ] [ - X ] + F [ - X ]
  F -> F F
  X (0.15) -> <leaves> F [ + F ] [ - F ]
}