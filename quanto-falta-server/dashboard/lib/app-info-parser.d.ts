declare module 'app-info-parser' {
  export default class AppInfoParser {
    constructor(file: File | string);
    parse(): Promise<{
      versionName: string;
      versionCode: string | number;
      package: string;
      [key: string]: any;
    }>;
  }
}
