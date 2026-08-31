/**
 * spark-md5 未提供官方类型声明，这里补充项目实际用到的最小声明。
 */
declare module 'spark-md5' {
  class SparkMD5 {
    append(data: ArrayBuffer | string): SparkMD5;
    end(raw?: boolean): string;
    destroy(): void;
    static hash(data: string, raw?: boolean): string;
    static hashBinary(data: string, raw?: boolean): string;
    static ArrayBuffer: new () => {
      append(data: ArrayBuffer): void;
      end(raw?: boolean): string;
      destroy(): void;
    };
  }

  export default SparkMD5;
}
