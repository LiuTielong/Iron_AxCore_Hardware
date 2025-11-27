
/*这个Config文件就像是项目的"设置中心"，它: 

1. __统一管理配置__: 所有模块使用相同的配置
2. __支持多种仿真器__: 从开源到商业仿真器
3. __标准化输出__: 让生成的Verilog代码规范易读
4. __灵活目录管理__: 不同模块可以生成到不同子目录
*/

package AxCore    // 定义这个文件属于AxCore包

import spinal.core._        // 导入SpinalHDL核心功能
import spinal.core.sim._    // 导入仿真相关功能
import spinal.sim.VCSFlags  // 导入VCS仿真器标志


object Config {             // 这是一个Scala单例对象，相当于Java中的静态类，全局只有一个实例。

  private var SubDir: String = ""

  def setGenSubDir(subdir: String): Unit = {    // 公共方法，用于设置生成代码的子目录
    SubDir = subdir
  }

  //主要硬件配置。
  def spinal = SpinalConfig(
    targetDirectory = "hw/gen/AxCore" + SubDir,
    defaultConfigForClockDomains = ClockDomainConfig(   // 时钟域配置
      resetKind = ASYNC,                                // 异步复位   
      clockEdge = RISING,                               // 上升沿时钟触发
      resetActiveLevel = LOW                            // 低电平复位
    ),
    onlyStdLogicVectorAtTopLevelIo = true,    // 顶层IO只使用std_logic_vector类型, 让生成的Verilog更标准化
    nameWhenByFile = false,                   // the generated Verilog codes will not have those "when_" wires
    anonymSignalPrefix = "tmp"                // use "tmp_" instead of "_zz_"
    // oneFilePerComponent = true,
  )

  // For Verilator Simulation
  def sim = SimConfig.withConfig(spinal).withFstWave                    // Verilator 仿真: 使用上面的硬件配置

  // For Iverilog Simulation
  def iverilogsim = SimConfig.withConfig(spinal).withIVerilog.withWave  // Iverilog 仿真（另一个开源仿真器）

  // For VCS Simulation
  def vcssim = SimConfig.withConfig(spinal).withVCS.withFSDBWave        // VCS 仿真（商业仿真器）

}
