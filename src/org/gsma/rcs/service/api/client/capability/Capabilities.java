/*
 * Copyright 2013, France Telecom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gsma.rcs.service.api.client.capability;


public class Capabilities
  implements android.os.Parcelable
{
  // Fields

  public static final android.os.Parcelable.Creator<Capabilities> CREATOR = null;

  // Constructors

  public Capabilities(){
  }
  public Capabilities(android.os.Parcel arg1){
  }
  // Methods

  public java.lang.String toString(){
    return (java.lang.String) null;
  }
  public void setTimestamp(long arg1){
  }
  public long getTimestamp(){
    return 0l;
  }
  public int describeContents(){
    return 0;
  }
  public void writeToParcel(android.os.Parcel arg1, int arg2){
  }
  public boolean isImageSharingSupported(){
    return false;
  }
  public void setImageSharingSupport(boolean arg1){
  }
  public boolean isVideoSharingSupported(){
    return false;
  }
  public void setVideoSharingSupport(boolean arg1){
  }
  public boolean isImSessionSupported(){
    return false;
  }
  public void setImSessionSupport(boolean arg1){
  }
  public boolean isFileTransferSupported(){
    return false;
  }
  public void setFileTransferSupport(boolean arg1){
  }
  public boolean isCsVideoSupported(){
    return false;
  }
  public void setCsVideoSupport(boolean arg1){
  }
  public boolean isPresenceDiscoverySupported(){
    return false;
  }
  public void setPresenceDiscoverySupport(boolean arg1){
  }
  public boolean isSocialPresenceSupported(){
    return false;
  }
  public void setSocialPresenceSupport(boolean arg1){
  }
  public boolean isFileTransferHttpSupported(){
    return false;
  }
  public void setFileTransferHttpSupport(boolean arg1){
  }
  public boolean isGeolocationPushSupported(){
    return false;
  }
  public void setGeolocationPushSupport(boolean arg1){
  }
  public void addSupportedExtension(java.lang.String arg1){
  }
  public java.util.ArrayList<java.lang.String> getSupportedExtensions(){
    return (java.util.ArrayList) null;
  }
}
