/*
ParamsGen implements a simple compensation: S - B2 + C2
Inputs:  S (scale factor in floating-point, assumed non-negative)
Outputs: S - B + C2 (B: bias of the FP format; C2: compensation constant)
*/

package AxCore.Basics

import spinal.core._
import spinal.core.sim._
import AxCore.Config
import scala.language.postfixOps
import scala.math.pow
import AxCore.Operators.AdderInt

// Generate (S - B2 + C2)
case class ParamsGen(ExpoWidth: Int, MantWidth: Int) extends Component {

  val TotalWidth = 1 + ExpoWidth + MantWidth
  val NegB = -((pow(2, ExpoWidth - 1) - 1) * pow(2, MantWidth)).toInt

  val io = new Bundle {
    val S_FP                  = in  Bits(TotalWidth bits)
    val S_minus_B2_plus_C2_TC = out Bits(TotalWidth bits)
  }
  noIoPrefix()

  // Compensation Stage 2 (k=0)
  val C2 = MantWidth match {
    case 10 => 58    // For E5M10, C2=58
    case 7  => 8     // For E8M7,  C2=8
    case _  => 0
  }

  // Constant for -(B2 - C2)
  val NegB2_plus_C2 = Bits(TotalWidth bits)
  NegB2_plus_C2 := S(NegB + C2, TotalWidth bits).asBits

  // Absolute value of input (drop sign): sign=0 + exponent + mantissa
  val S_Abs_TC = B("0") ## io.S_FP(TotalWidth - 2 downto 0)

  // S - B2 + C2
  val adder1 = new AdderInt(Width = TotalWidth)
  adder1.io.X := S_Abs_TC
  adder1.io.Y := NegB2_plus_C2

  io.S_minus_B2_plus_C2_TC := adder1.io.Sum

}

object ParamsGen_Gen extends App {
  Config.setGenSubDir("/ParamsGen")
  Config.spinal.generateVerilog(ParamsGen(ExpoWidth = 5, MantWidth = 10)).printRtl().mergeRTLSource()
  // Config.spinal.generateVerilog(ParamsGen(ExpoWidth = 8, MantWidth = 7)).printRtl().mergeRTLSource()
}

object ParamsGen_Sim extends App {
  // Prepare test data
  val S = 2
  val S_FPBin = FP2BinCvt.FloatToFPAnyBin(f = S, ExpoWidth = 5, MantWidth = 10)

  // Use Icarus Verilog for open-source simulation
  Config.iverilogsim.compile(ParamsGen(ExpoWidth = 5, MantWidth = 10)).doSim { dut =>
    dut.clockDomain.forkStimulus(2)

    for (clk <- 0 until 100) {
      if (clk >= 10 && clk < 90) {
        dut.io.S_FP #= BigInt(S_FPBin.replace("_", ""), 2)
      } else {
        dut.io.S_FP #= 0
      }
      dut.clockDomain.waitRisingEdge()
    }
    sleep(50)
  }
}
