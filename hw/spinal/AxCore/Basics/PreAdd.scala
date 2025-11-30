/*
Run: T = A - B1 + C1
*/
package AxCore.Basics

import spinal.core._
import spinal.core.sim._
import AxCore.Config
import scala.language.postfixOps
import AxCore.Operators.AdderInt
import scala.math.pow


// MARK: PreAdd Unit for A - B1 + C1
// QtTotalWidth: bits of weight. (W4 or W8)

case class PreAdd(QtTotalWidth : Int, ExpoWidth: Int, MantWidth: Int) extends Component {

  val TotalWidth = 1 + ExpoWidth + MantWidth

  val io = new Bundle {
    val A_FP      = in  Bits(TotalWidth bits)
    val Wq_FmtSel = in  Bits(2 bits)                 // Wq Format Select. 00,01 for E3M0, 10 for E2M1, 11 for E1M2

    val T_TC      = out Bits(TotalWidth bits)        // T in Two's Complement
    val A_Valid   = out Bool()                        // Whether A is zero.
  }
  noIoPrefix()


  // * Compensation Stage 1 (k=0)
  val C1 = MantWidth match {
    case 10 => 43    // For A in E5M10, C1=43
    case 7  => 5     // For A in E8M7,  C1=5
    case _  => 0
  }


  // * -B1 = [2^(QtExpoWidth-1)-1] << MantWidth
  // define a function.
  def QtNegB(QtExpoWidth: Int): Int = {
    -((pow(2, QtExpoWidth-1) - 1) * pow(2, MantWidth)).toInt
  }

  val C1_minus_B1 = Bits(TotalWidth bits)

  if (QtTotalWidth == 4) {    // * For W4
    switch(io.Wq_FmtSel) {
      is (B"00") { C1_minus_B1 := S(C1+QtNegB(3), TotalWidth bits).asBits }    // For Wq in E3M0
      is (B"01") { C1_minus_B1 := S(C1+QtNegB(3), TotalWidth bits).asBits }    // For Wq in E3M0
      is (B"10") { C1_minus_B1 := S(C1+QtNegB(2), TotalWidth bits).asBits }    // For Wq in E2M1
      is (B"11") { C1_minus_B1 := S(C1+QtNegB(1), TotalWidth bits).asBits }    // For Wq in E1M2
    }
  } else if (QtTotalWidth == 8) {    // * For W8
    C1_minus_B1 := S(C1+QtNegB(4), TotalWidth bits).asBits
  }


  // * A - B1 + C1
  val Adder1 = AdderInt(Width=TotalWidth)
  Adder1.io.X := io.A_FP
  Adder1.io.Y := C1_minus_B1

  io.T_TC := Adder1.io.Sum

  io.A_Valid := io.A_FP.orR    // Reduced OR

}


object PreAddGen extends App {
 val QtTotalWidth = 4
//   val QtTotalWidth = 8
  val ExpoWidth    = 5
  val MantWidth    = 10
  Config.setGenSubDir("/PreAdd")
  Config.spinal.generateVerilog(PreAdd(QtTotalWidth=QtTotalWidth, ExpoWidth=ExpoWidth, MantWidth=MantWidth)).printRtl().mergeRTLSource()
}

object PreAdd_Sim extends App {
  // ===== 1. Prepare test data =====
  // Test activation value in float
  val A = 2.0f

  // Convert float to custom FP format (same helper as used in ParamsGen_Sim)
  val A_FPBin   = FP2BinCvt.FloatToFPAnyBin(f = A, ExpoWidth = 5, MantWidth = 10)
  val A_FPValue = BigInt(A_FPBin.replace("_", ""), 2)

  // ===== 2. Run simulation with Icarus Verilog =====
  Config.iverilogsim.compile(PreAdd(QtTotalWidth = 4, ExpoWidth = 5, MantWidth = 10)).doSim { dut =>
    // Create clock stimulus with period = 2 simulation time units
    dut.clockDomain.forkStimulus(2)

    // Initialize inputs
    dut.io.A_FP      #= 0
    dut.io.Wq_FmtSel #= 0

    for (clk <- 0 until 100) {
      if (clk >= 10 && clk < 90) {
        // Drive A_FP with the FP16 encoding of 2.0f
        dut.io.A_FP #= A_FPValue

        // Select Wq format:
        // 0 / 1 -> E3M0, 2 -> E2M1, 3 -> E1M2
        // Here we test E1M2 (B"11")
        dut.io.Wq_FmtSel #= 3
      } else {
        // Outside active window, drive zeros
        dut.io.A_FP      #= 0
        dut.io.Wq_FmtSel #= 0
      }

      // Wait for rising edge of the clock
      dut.clockDomain.waitRisingEdge()

      // Print key signals for debugging
      println(
        f"clk=$clk%3d  " +
        f"A_Valid=${dut.io.A_Valid.toBoolean}  " +
        f"A_FP=0x${dut.io.A_FP.toBigInt.toString(16)}%4s  " +
        f"Wq_FmtSel=${dut.io.Wq_FmtSel.toBigInt}%1d  " +
        f"T_TC=0x${dut.io.T_TC.toBigInt.toString(16)}"
      )
    }

    sleep(50)
  }
}