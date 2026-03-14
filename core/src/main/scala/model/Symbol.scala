package model

enum Symbol:
  case DrawForward, MoveForward
  case TurnLeft, TurnRight, ReverseDirection
  case StackPush, StackPop
  case IncrementLineWidth, DecrementLineWidth
  case Dot
  case ScaleUpLineWidth, ScaleDownLineWidth
  case IncrementTurningAngle, DecrementTurningAngle
  case ColorChange(color: String)