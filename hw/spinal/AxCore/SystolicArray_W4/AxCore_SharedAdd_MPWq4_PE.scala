package AxCore.SystolicArray_W4

import spinal.core._
import spinal.core.sim._
import AxCore.Config
import scala.language.postfixOps
import AxCore.Basics.{GuardAW, SNC_W4}
import AxCore.Operators.AdderInt


// MARK: AxCore's Weight Stationary Two-Level Spatial Array, with Shared Partial Sum Adder -- PE
// Weight Stationary + SNC_W4 + FPMA + GuardAW
case class AxCore_SharedAdd_MPWq4_PE(
                                     QtTotalWidth : Int,
                                     ExpoWidth    : Int,
                                     MantWidth    : Int,
                                    ) extends Component {

  val TotalWidth = 1 + ExpoWidth + MantWidth

  // 1. Interface and bit width.
  val io = new Bundle {
    val Wq_CIN_FP  = in  Bits(QtTotalWidth bits)               // Wq Cascade In
    val Wq_COUT_FP = out Bits(QtTotalWidth bits)               // Wq Cascade Out
    val Wq_FmtSel  = in  Bits(2 bits)                          // Wq Format Select. 00,01 for E3M0, 10 for E2M1, 11 for E1M2

    val T_CIN_TC   = in  Bits(TotalWidth bits)                 // T Cascade In  (in Two's Complement)
    val T_COUT_TC  = out Bits(TotalWidth bits)                 // T Cascade Out (in Two's Complement)
    val A_Vld_CIN  = in  Bool()                                // Validness of A_FP Cascade In
    val A_Vld_COUT = out Bool()                                // Validness of A_FP Cascade Out

    val R_FP       = out Bits(TotalWidth bits)  simPublic()    // R = Wq * A (R=T+Align(Wq))

    val WqLock     = in  Bool()                                // Locking the preloaded Wq, implement "weight stationary"
  }
  noIoPrefix()


  // 2. Weight Stationary
  // if WqLock is True, Wq is locked.
  // if Wq is false, accept a new Wq from previous PE.
  val WqLockReg = Reg(Bits(QtTotalWidth bits)).init(B(0))

  when(io.WqLock) {
    WqLockReg := WqLockReg
  } otherwise {
    WqLockReg := io.Wq_CIN_FP
  }


  // 3. Subnormal Conversion
  val SNC_MPWq4 = new SNC_W4()
  SNC_MPWq4.io.Wq_FP_In := WqLockReg
  SNC_MPWq4.io.Wq_FmtSel := io.Wq_FmtSel  
  SNC_MPWq4.io.StochasticBit := io.T_CIN_TC(MantWidth-1)  // use 1 bit of TC as StochasticBit to launch "NeedRandomize" of SNC
  val WqGuarded = SNC_MPWq4.io.Wq_FP_Out                  // {S1, E3, M2}, totally 6 bits


  // 4. Extend the guarded Wq to bigger FP (E3M2 to FP16)
  val Wq_Extend_FP = WqGuarded(5) ## B(0, ExpoWidth-3 bits) ## WqGuarded(4 downto 0) ## B(0, MantWidth-2 bits) // {1bit, 2bits, 5bits, 8bits}


  // 5. Approximate Multiplier Stage1
  val AxMultS1 = new AdderInt(Width=TotalWidth)
  AxMultS1.io.X := Wq_Extend_FP
  AxMultS1.io.Y := io.T_CIN_TC


  //  6. Guarding the result of A * Wq
  val GuardingAW = new GuardAW(TotalWidth=TotalWidth)
  GuardingAW.io.AW_In := AxMultS1.io.Sum
  GuardingAW.io.Wq_NotZero := SNC_MPWq4.io.Wq_NotZero
  GuardingAW.io.A_Valid := io.A_Vld_CIN


  // Approximated R = Wq * A
  io.R_FP := GuardingAW.io.AW_Out


  // 7. Cascade output
  io.T_COUT_TC := io.T_CIN_TC       // no pipeline
  io.A_Vld_COUT := io.A_Vld_CIN     // no pipeline
  io.Wq_COUT_FP := WqLockReg        // with pipeline

  // 8. MARK: The Shared Partial Sum Adder is part of the PE, it is realized in the Tile level Component in our code.

}



object AxCore_SharedAdd_MPWq4_PE_Gen extends App {
   val ExpoWidth = 5
   val MantWidth = 10
  // val ExpoWidth = 8
  // val MantWidth = 7
  // val ExpoWidth = 8
  // val MantWidth = 23

  Config.setGenSubDir(s"/PE/MPWq4_E${ExpoWidth}M${MantWidth}")
  Config.spinal.generateVerilog(AxCore_SharedAdd_MPWq4_PE(
    QtTotalWidth=4, ExpoWidth=ExpoWidth, MantWidth=MantWidth
  )).printRtl().mergeRTLSource()
}

/*
Questions: 
1. Why the transimission of T needs no pipeline?
2. Why the transimission of Wq needs pipeline?
*/