// Generator : SpinalHDL v1.10.2a    git head : a348a60b7e8b6a455c72e1536ec3d74a2ea16935
// Component : PreAdd
// Git hash  : a117b371482394e34c3d217231d937414f8d5291

`timescale 1ns/1ps

module PreAdd (
  input  wire [15:0]   A_FP,
  input  wire [1:0]    Wq_FmtSel,
  output wire [15:0]   T_TC,
  output wire          A_Valid
);

  wire       [15:0]   Adder1_Sum;
  reg        [15:0]   C1_minus_B1;

  AdderInt Adder1 (
    .X   (A_FP[15:0]       ), //i
    .Y   (C1_minus_B1[15:0]), //i
    .Sum (Adder1_Sum[15:0] )  //o
  );
  always @(*) begin
    case(Wq_FmtSel)
      2'b00 : begin
        C1_minus_B1 = 16'hf42b;
      end
      2'b01 : begin
        C1_minus_B1 = 16'hf42b;
      end
      2'b10 : begin
        C1_minus_B1 = 16'hfc2b;
      end
      default : begin
        C1_minus_B1 = 16'h002b;
      end
    endcase
  end

  assign T_TC = Adder1_Sum;
  assign A_Valid = (|A_FP);

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
