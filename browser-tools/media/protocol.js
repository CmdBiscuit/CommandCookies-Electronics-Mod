'use strict';
// Java's DataInputStream uses modified UTF-8, including UTF-16 surrogate pairs.
function encodeUTF(text){
  const bytes=[];
  for(let i=0;i<text.length;i++){
    const c=text.charCodeAt(i);
    if(c>0&&c<128)bytes.push(c);
    else if(c<2048)bytes.push(192|(c>>6),128|(c&63));
    else bytes.push(224|(c>>12),128|((c>>6)&63),128|(c&63));
  }
  if(bytes.length>65535)throw new Error('Browser message exceeds its limit');
  const result=Buffer.alloc(bytes.length+2);result.writeUInt16BE(bytes.length);Buffer.from(bytes).copy(result,2);return result;
}
function decodeUTF(bytes){
  const chars=[];
  for(let i=0;i<bytes.length;){
    const c=bytes[i++];
    if(c>0&&c<128)chars.push(c);
    else if((c&224)===192){if(i>=bytes.length||(bytes[i]&192)!==128)throw new Error('Invalid browser text');const d=bytes[i++];const value=((c&31)<<6)|(d&63);if(value!==0&&value<128)throw new Error('Invalid browser text');chars.push(value);}
    else if((c&240)===224){if(i+1>=bytes.length||(bytes[i]&192)!==128||(bytes[i+1]&192)!==128)throw new Error('Invalid browser text');const value=((c&15)<<12)|((bytes[i++]&63)<<6)|(bytes[i++]&63);if(value<2048)throw new Error('Invalid browser text');chars.push(value);}
    else throw new Error('Invalid browser text');
  }
  return String.fromCharCode(...chars);
}
class Input {
  constructor(onCommand,onError){this.buffer=Buffer.alloc(0);this.onCommand=onCommand;this.onError=onError;}
  accept(chunk){
    try{
      if(this.buffer.length+chunk.length>262144)throw new Error('Browser input exceeds its limit');
      this.buffer=Buffer.concat([this.buffer,chunk]);
      while(this.buffer.length>=2){const size=this.buffer.readUInt16BE();if(size>24576)throw new Error('Browser input exceeds its limit');if(this.buffer.length<size+2)break;const text=decodeUTF(this.buffer.subarray(2,size+2));this.buffer=this.buffer.subarray(size+2);if(text.length>8192)throw new Error('Browser command exceeds its limit');const value=JSON.parse(text);if(!value||typeof value!=='object'||Array.isArray(value))throw new Error('Invalid browser command');this.onCommand(value);}
    }catch(error){this.onError(error);}
  }
}
module.exports={encodeUTF,decodeUTF,Input};
