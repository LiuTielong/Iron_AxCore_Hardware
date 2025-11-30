// Generator : SpinalHDL v1.10.2a    git head : a348a60b7e8b6a455c72e1536ec3d74a2ea16935
// Component : NormalizeTop
// Git hash  : a117b371482394e34c3d217231d937414f8d5291

`timescale 1ns/1ps

module NormalizeTop (
  input  wire [24:0]   Src,
  output wire [15:0]   Result,
  input  wire          clk
);

  wire       [15:0]   Norm_result;

  normalize #(
    .E        (5 ),
    .M        (10),
    .INTEGER  (4 ),
    .FRACTION (15)
  ) Norm (
    .clk    (clk              ), //i
    .src    (Src[24:0]        ), //i
    .result (Norm_result[15:0])  //o
  );
  assign Result = Norm_result;

endmodule
