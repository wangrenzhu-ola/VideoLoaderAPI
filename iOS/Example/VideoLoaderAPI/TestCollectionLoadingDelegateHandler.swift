//
//  TestCollectionLoadingDelegateHandler.swift
//  TestVideoLoader
//
//  Created by wushengtao on 2023/9/3.
//

import VideoLoaderAPI

class TestCollectionLoadingDelegateHandler: AGCollectionLoadingDelegateHandler {
    var selectClosure: ((IndexPath)->())?
    open func collectionView(_ collectionView: UICollectionView, didHighlightItemAt indexPath: IndexPath) {
        debugLoaderPrint("[UI]didHighlightItemAt: \(indexPath.row)")
    }
    public func collectionView(_ collectionView: UICollectionView, didUnhighlightItemAt indexPath: IndexPath) {
        debugLoaderPrint("[UI]didUnhighlightItemAt: \(indexPath.row)")
    }
    
    open func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        debugLoaderPrint("[UI]didSelectItemAt: \(indexPath.row)")
        selectClosure?(indexPath)
    }
}
