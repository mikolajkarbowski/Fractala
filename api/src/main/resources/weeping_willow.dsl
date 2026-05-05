Config {
  lineLength: 20.0
  turningAngle: 35.0
  maxIterations: 8
  startingColor: wood
}

Colors {
  wood: 0.35, 0.25, 0.15
  willowGreen: 0.30, 0.70, 0.30
  paleYellow: 0.85, 0.85, 0.40
}

Axiom: X

Rules {
  X (0.4) -> <wood> F [ + X ] [ - X ] F [ - <willowGreen> X ]
  X (0.4) -> <wood> F [ - X ] [ + X ] F [ + <paleYellow> X ]
  X (0.2) -> <wood> F [ + X ] [ - X ]
  F -> F F
}