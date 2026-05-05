Config {
  lineLength: 30.0
  turningAngle: 22.5
  maxIterations: 7
  startingColor: stalk
}

Colors {
  stalk: 0.45, 0.35, 0.15
  autumnRed: 0.85, 0.20, 0.15
  autumnGold: 0.95, 0.65, 0.10
}

Axiom: X

Rules {
  X (0.5) -> <stalk> F - [ [ X ] + X ] + F [ + <autumnRed> F X ] - X
  X (0.5) -> <stalk> F - [ [ X ] + X ] + F [ + <autumnGold> F X ] - X
  F -> F F
}