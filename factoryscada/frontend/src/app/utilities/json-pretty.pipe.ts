import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ 
    name: 'jsonPrettyHtml',
    standalone: true 
 })
export class JsonPrettyHtmlPipe implements PipeTransform {
  transform(value: any): string {
    if (!value) return '';
    const jsonObject: any = JSON.parse(value);
    // pretty-print JSON
    const json = JSON.stringify(jsonObject, null, 2);
    // replace each line break with <br> and spaces with &nbsp; for indentation
    return json
      .replace(/ /g, '&nbsp;')  // preserve indentation
      .replace(/\\\n/g, '<br>');  // line breaks
  }
}
