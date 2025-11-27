// Generator : SpinalHDL v1.10.2a    git head : a348a60b7e8b6a455c72e1536ec3d74a2ea16935
// Component : ParamsGen

`timescale 1ns/1ps

module ParamsGen (
  input  wire [15:0]   S_FP,
  output wire [15:0]   S_minus_B2_plus_C2_TC
);

  wire       [15:0]   adder1_Sum;
  wire       [15:0]   NegB2_plus_C2;
  wire       [15:0]   S_Abs_TC;

  AdderInt adder1 (
    .X   (S_Abs_TC[15:0]     ), //i
    .Y   (NegB2_plus_C2[15:0]), //i
    .Sum (adder1_Sum[15:0]   )  //o
  );
  assign NegB2_plus_C2 = 16'hc43a;
  assign S_Abs_TC = {1'b0,S_FP[14 : 0]};
  assign S_minus_B2_plus_C2_TC = adder1_Sum;

endmodule

module AdderInt (
  input  wire [15:0]   X,
  input  wire [15:0]   Y,
  output wire [15:0]   Sum
);

  wire       [15:0]   SumSameWidth;

  assign SumSameWidth = (X + Y);
  assign Sum = SumSameWidth;

endmodule
